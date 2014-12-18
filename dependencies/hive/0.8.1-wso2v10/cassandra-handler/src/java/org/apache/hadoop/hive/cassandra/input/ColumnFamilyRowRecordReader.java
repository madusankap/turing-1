package org.apache.hadoop.hive.cassandra.input;

import com.google.common.collect.*;
import org.apache.cassandra.auth.IAuthenticator;
import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.hadoop.ColumnFamilyRecordReader;
import org.apache.cassandra.hadoop.ColumnFamilySplit;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.cassandra.utils.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.cassandra.serde.AbstractColumnSerDe;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ColumnFamilyRowRecordReader extends ColumnFamilyRecordReader {

    private static final Logger logger = LoggerFactory.getLogger(ColumnFamilyRecordReader.class);

    public static final int CASSANDRA_HADOOP_MAX_KEY_SIZE_DEFAULT = 8192;

    private ColumnFamilySplit split;
    private IncrementalColumnFamilySplit incSplit;
    private RowIterator iter;
    private Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> currentRow;
    private SlicePredicate predicate;
    private boolean isEmptyPredicate;
    private int totalRowCount; // total number of rows to fetch
    private int batchSize; // fetch this many per batch
    private String cfName;
    private String indexCfName;
    private String keyspace;
    private String indexKeySpace;
    private TSocket socket;
    private TSocket indexClientSocket;
    private Cassandra.Client client;
    private Cassandra.Client indexClient;
    private ConsistencyLevel consistencyLevel;
    private int keyBufferSize = 8192;
    private List<IndexExpression> filter;
    private AuthenticationRequest authRequest;
    private boolean isCFIncremental;

    public ColumnFamilyRowRecordReader() {
        this(ColumnFamilyRecordReader.CASSANDRA_HADOOP_MAX_KEY_SIZE_DEFAULT);
    }

    public ColumnFamilyRowRecordReader(int keyBufferSize) {
        super();
        this.keyBufferSize = keyBufferSize;
    }

    public void close() {
        if (socket != null && socket.isOpen()) {
            socket.close();
            socket = null;
            client = null;
        }
        if (indexClientSocket != null && indexClientSocket.isOpen()) {
            indexClientSocket.close();
            indexClient = null;
            indexClientSocket = null;
        }
    }

    public ByteBuffer getCurrentKey() {
        return currentRow.left;
    }

    public SortedMap<ByteBuffer, IColumn> getCurrentValue() {
        return currentRow.right;
    }

    public float getProgress() {
        // TODO this is totally broken for wide rows
        // the progress is likely to be reported slightly off the actual but close enough
        float progress = ((float) iter.rowsRead() / totalRowCount);
        return progress > 1.0F ? 1.0F : progress;
    }

    static boolean isEmptyPredicate(SlicePredicate predicate) {
        if (predicate == null)
            return true;

        if (predicate.isSetColumn_names() && predicate.getSlice_range() == null)
            return false;

        if (predicate.getSlice_range() == null)
            return true;

        byte[] start = predicate.getSlice_range().getStart();
        if ((start != null) && (start.length > 0))
            return false;

        byte[] finish = predicate.getSlice_range().getFinish();
        if ((finish != null) && (finish.length > 0))
            return false;

        return true;
    }

    public void initialize(InputSplit split, TaskAttemptContext context) throws IOException {
        Configuration conf = context.getConfiguration();

        cfName = ConfigHelper.getInputColumnFamily(conf);
        keyspace = ConfigHelper.getInputKeyspace(conf);

        String hiveIncrementalTableConfValue = keyspace + "::" + cfName;
        boolean isIncremental = Boolean.valueOf(conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_PROCESS_ENABLE.toString()));
        if (isIncremental) {
            String hiveIncrementalCassandraTables = conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_CASSANDRA_TABLES.toString());
            isCFIncremental = false;
            if (null != hiveIncrementalCassandraTables && !hiveIncrementalCassandraTables.equals("")) {
                String[] casssandraTables = hiveIncrementalCassandraTables.split(",");
                for (String cassandraCF : casssandraTables) {
                    if (!cassandraCF.trim().equals("")) {
                        cassandraCF.trim().equalsIgnoreCase(hiveIncrementalTableConfValue);
                        isCFIncremental = true;
                        break;
                    }
                }
            }
            if (isCFIncremental) {
                indexCfName = IncrementalStaticRowIterator.EVENT_INDEX_TABLE_PREFIX + cfName;
                indexKeySpace = conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_PROCESS_KEYSPACE.toString());
                incSplit = (IncrementalColumnFamilySplit) split;

            } else {
                this.split = (ColumnFamilySplit) split;
            }
        } else {
            this.split = (ColumnFamilySplit) split;
        }


        KeyRange jobRange = ConfigHelper.getInputKeyRange(conf);
        filter = jobRange == null ? null : jobRange.row_filter;
        predicate = ConfigHelper.getInputSlicePredicate(conf);
        boolean widerows = ConfigHelper.getInputIsWide(conf);
        isEmptyPredicate = isEmptyPredicate(predicate);
        totalRowCount = ConfigHelper.getInputSplitSize(conf);
        batchSize = ConfigHelper.getRangeBatchSize(conf);


        consistencyLevel = ConsistencyLevel.valueOf(ConfigHelper.getReadConsistencyLevel(conf));


        try {
            // only need to connect once
            if (socket != null && socket.isOpen())
                return;

            // create connection using thrift
            String location = getLocation(conf);

            // This is a temporary fix for Cassandra SSL issue. We are switching secure and non-secure socket based on
            // trustStore system property set on wso2carbon.sh file.
            String trustStore = conf.get(AbstractColumnSerDe.SSL_TRUSTSTORE, null);
            String trustStorePassword = conf.get(AbstractColumnSerDe.SSL_TRUSTSTORE_PASSWORD, null);

            if ((trustStore != null && !trustStore.isEmpty()) && (trustStorePassword != null && !trustStorePassword.isEmpty())) {

                TSSLTransportFactory.TSSLTransportParameters
                        tsslTransportParameters = new TSSLTransportFactory.TSSLTransportParameters();
                tsslTransportParameters.setTrustStore(trustStore, trustStorePassword);

                socket = TSSLTransportFactory.getClientSocket(
                        location, ConfigHelper.getInputRpcPort(conf),
                        AbstractColumnSerDe.SSL_TIME_OUT_VALUE, tsslTransportParameters);
            } else {
                socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
            }

            TBinaryProtocol binaryProtocol = new TBinaryProtocol(new TFramedTransport(socket), true, true);
            client = new Cassandra.Client(binaryProtocol);

            if ((trustStore == null || trustStore.isEmpty()) && (trustStorePassword == null || trustStorePassword.isEmpty())) {
                socket.open();
            }

            // log in
            client.set_keyspace(keyspace);
            if (ConfigHelper.getInputKeyspaceUserName(conf) != null) {
                Map<String, String> creds = new HashMap<String, String>();
                creds.put(IAuthenticator.USERNAME_KEY, ConfigHelper.getInputKeyspaceUserName(conf));
                creds.put(IAuthenticator.PASSWORD_KEY, ConfigHelper.getInputKeyspacePassword(conf));
                authRequest = new AuthenticationRequest(creds);
                client.login(authRequest);
            }

            if (isCFIncremental) {
                if (indexClientSocket != null && indexClientSocket.isOpen())
                    return;

                if ((trustStore != null && !trustStore.isEmpty()) && (trustStorePassword != null && !trustStorePassword.isEmpty())) {

                    TSSLTransportFactory.TSSLTransportParameters
                            tsslTransportParameters = new TSSLTransportFactory.TSSLTransportParameters();
                    tsslTransportParameters.setTrustStore(trustStore, trustStorePassword);

                    indexClientSocket = TSSLTransportFactory.getClientSocket(
                            location, ConfigHelper.getInputRpcPort(conf),
                            AbstractColumnSerDe.SSL_TIME_OUT_VALUE, tsslTransportParameters);
                } else {
                    indexClientSocket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
                }

                // create connection using thrift
                //indexClientSocket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
                TBinaryProtocol indexBinaryProtocol = new TBinaryProtocol(new TFramedTransport(indexClientSocket), true, true);
                indexClient = new Cassandra.Client(indexBinaryProtocol);

                if ((trustStore == null || trustStore.isEmpty()) && (trustStorePassword == null || trustStorePassword.isEmpty())) {
                    indexClientSocket.open();
                }

                indexClient.set_keyspace(indexKeySpace);

                Map<String, String> creds = new HashMap<String, String>();
                creds.put(IAuthenticator.USERNAME_KEY, conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_USERNAME.toString()));
                creds.put(IAuthenticator.PASSWORD_KEY, conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_PASSWORD.toString()));
                AuthenticationRequest authRequest = new AuthenticationRequest(creds);
                indexClient.login(authRequest);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (widerows) {
            iter = new WideRowIterator(conf);
        } else {
            if (isCFIncremental) {
                IncrementalStaticRowIterator incrIter = new IncrementalStaticRowIterator(conf);
                iter = incrIter;
                incrIter.maxRowsBlock = Integer.parseInt(conf.get(HiveConf.ConfVars.HIVE_CASSANDRA_MAX_FETCH_ROW_SIZE.toString()));
                incrIter.maxRows = Integer.parseInt(conf.get(HiveConf.ConfVars.HIVE_CASSANDRA_MAX_ROW_SIZE.toString()));
                incrIter.maximumCassandraConnectionRetries = Integer.parseInt(conf.get(HiveConf.ConfVars.HIVE_CASSANDRA_INCREMENTAL_FETCH_RETRIES.toString()));
            } else {
                iter = new StaticRowIterator(conf);
            }

        }
        logger.debug("created {}", iter);
    }

    public boolean nextKeyValue() throws IOException {
        if (!iter.hasNext())
            return false;
        currentRow = iter.next();
        return true;
    }

    // we don't use endpointsnitch since we are trying to support hadoop nodes that are
    // not necessarily on Cassandra machines, too.  This should be adequate for single-DC clusters, at least.
    private String getLocation(Configuration conf) {
        ArrayList<InetAddress> localAddresses = new ArrayList<InetAddress>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements())
                localAddresses.addAll(Collections.list(nets.nextElement().getInetAddresses()));
        } catch (SocketException e) {
            throw new AssertionError(e);
        }

        for (InetAddress address : localAddresses) {
            String[] locations;
            try {
                if (isCFIncremental)
                    locations = incSplit.getLocations();
                else
                    locations = split.getLocations();

                for (String location : locations) {
                    InetAddress locationAddress = null;
                    try {
                        locationAddress = InetAddress.getByName(location);
                    } catch (UnknownHostException e) {
                        throw new AssertionError(e);
                    }
                    if (address.equals(locationAddress)) {
                      try {
                         TSocket socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
                         socket.open();
                         return location;
                     } catch (Exception ignored) {
                         //ignore. move to next host in the list
                         if (logger.isDebugEnabled()) {
                             logger.debug("Host " + location + " seems to be down. Trying next hosts in " +
                                     "the list..", ignored);
                         }
                     }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String trustStore = conf.get(AbstractColumnSerDe.SSL_TRUSTSTORE, null);
        String trustStorePassword = conf.get(AbstractColumnSerDe.SSL_TRUSTSTORE_PASSWORD, null);

        TSSLTransportFactory.TSSLTransportParameters
                tsslTransportParameters = new TSSLTransportFactory.TSSLTransportParameters();

        if ((trustStore != null && !trustStore.isEmpty()) &&
                (trustStorePassword != null && !trustStorePassword.isEmpty())) {
            tsslTransportParameters.setTrustStore(trustStore, trustStorePassword);
        }

        if(isCFIncremental){
            try {
                for (String location : incSplit.getLocations()) {
                     try {
                         TSocket socket;
                         if ((trustStore != null && !trustStore.isEmpty()) && (trustStorePassword != null && !trustStorePassword.isEmpty())) {
                             socket = TSSLTransportFactory.getClientSocket(
                                     location, ConfigHelper.getInputRpcPort(conf),
                                     AbstractColumnSerDe.SSL_TIME_OUT_VALUE, tsslTransportParameters);
                         } else {
                             socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
                             socket.open();
                         }
//                         TSocket socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
//                         socket.open();
                         return location;
                     } catch (Exception ignored) {
                         //ignore. move to next host in the list
                         if (logger.isDebugEnabled()) {
                             logger.debug("Host " + location + " seems to be down. Trying next hosts in " +
                                     "the list..", ignored);
                         }
                     }
                 }
                 return incSplit.getLocations()[0];
            } catch (IOException e) {
                 throw new RuntimeException(e);
            }

        }
        else {
            for (String location : split.getLocations()) {
                try {
                    TSocket socket;
                    if ((trustStore != null && !trustStore.isEmpty()) && (trustStorePassword != null && !trustStorePassword.isEmpty())) {
                        socket = TSSLTransportFactory.getClientSocket(
                                location, ConfigHelper.getInputRpcPort(conf),
                                AbstractColumnSerDe.SSL_TIME_OUT_VALUE, tsslTransportParameters);
                    } else {
                        socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
                        socket.open();
                    }
//                    TSocket socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
//                    socket.open();
                    return location;
                } catch (Exception ignored) {
                    //ignore. move to next host in the list
                    if (logger.isDebugEnabled()) {
                        logger.debug("Host " + location + " seems to be down. Trying next hosts in " +
                                "the list..", ignored);
                    }
                }
            }
            return split.getLocations()[0];
        }
    }

    private abstract class RowIterator extends AbstractIterator<Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>>> {
        protected List<KeySlice> rows;
        protected int totalRead = 0;
        protected final AbstractType<?> comparator;
        protected final AbstractType<?> subComparator;
        protected final IPartitioner partitioner;

        private RowIterator(Configuration conf) {
            try {
                partitioner = FBUtilities.newPartitioner(client.describe_partitioner());

                // Get the Keyspace metadata, then get the specific CF metadata
                // in order to populate the sub/comparator.
                Map<String, String> creds = new HashMap<String, String>();
                creds.put(IAuthenticator.USERNAME_KEY, ConfigHelper.getInputKeyspaceUserName(conf));
                creds.put(IAuthenticator.PASSWORD_KEY, ConfigHelper.getInputKeyspacePassword(conf));
                authRequest = new AuthenticationRequest(creds);
                client.login(authRequest);

                KsDef ks_def = client.describe_keyspace(keyspace);
                List<String> cfnames = new ArrayList<String>();
                for (CfDef cfd : ks_def.cf_defs)
                    cfnames.add(cfd.name);

                int idx = cfnames.indexOf(cfName);

                CfDef cf_def = ks_def.cf_defs.get(idx);
                comparator = TypeParser.parse(cf_def.comparator_type);
                subComparator = cf_def.subcomparator_type == null ? null : TypeParser.parse(cf_def.subcomparator_type);

            } catch (ConfigurationException e) {
                throw new RuntimeException("unable to load sub/comparator", e);
            } catch (TException e) {
                throw new RuntimeException("error communicating via Thrift", e);
            } catch (Exception e) {
                throw new RuntimeException("unable to load keyspace " + keyspace, e);
            }
        }


        /**
         * @return total number of rows read by this record reader
         */
        public int rowsRead() {
            return totalRead;
        }

        protected IColumn unthriftify(ColumnOrSuperColumn cosc) {
            if (cosc.counter_column != null)
                return unthriftifyCounter(cosc.counter_column);
            if (cosc.counter_super_column != null)
                return unthriftifySuperCounter(cosc.counter_super_column);
            if (cosc.super_column != null)
                return unthriftifySuper(cosc.super_column);
            assert cosc.column != null;
            return unthriftifySimple(cosc.column);
        }

        private IColumn unthriftifySuper(SuperColumn super_column) {
            org.apache.cassandra.db.SuperColumn sc = new org.apache.cassandra.db.SuperColumn(super_column.name, subComparator);
            for (Column column : super_column.columns) {
                sc.addColumn(unthriftifySimple(column));
            }
            return sc;
        }

        protected IColumn unthriftifySimple(Column column) {
            return new org.apache.cassandra.db.Column(column.name, column.value, column.timestamp);
        }

        private IColumn unthriftifyCounter(CounterColumn column) {
            //CounterColumns read the nodeID from the System table, so need the StorageService running and access
            //to cassandra.yaml. To avoid a Hadoop needing access to yaml return a regular Column.
            return new org.apache.cassandra.db.Column(column.name, ByteBufferUtil.bytes(column.value), 0);
        }

        private IColumn unthriftifySuperCounter(CounterSuperColumn superColumn) {
            org.apache.cassandra.db.SuperColumn sc = new org.apache.cassandra.db.SuperColumn(superColumn.name, subComparator);
            for (CounterColumn column : superColumn.columns)
                sc.addColumn(unthriftifyCounter(column));
            return sc;
        }
    }

     private class IncrementalStaticRowIterator extends RowIterator {
        private LinkedBlockingQueue<KeySlice> rows;
        protected int i = 0;
        private static final String EVENT_INDEX_TABLE_PREFIX = "event_index_";

        protected final AbstractType<?> indexCFComparator;
        protected final AbstractType<?> indexCFSubComparator;


        private long lastAccessedIndexColumnName = -1;
//        private int numberOfRowsLoaded = 0;
        private long lastAccessedIndexRowKey = -1;
        private int maxRows = 100000;
        private int minRows=1000;
        private int maxRowsBlock = 10000;
        private int maximumCassandraConnectionRetries = 3;

        private boolean isfirstFetch = true;

        private AtomicBoolean isCassandraRowsLoaderRunning = new AtomicBoolean();
        private AtomicBoolean isCompletedFetching = new AtomicBoolean();
        private ExecutorService cassandraLoaderService = Executors.newSingleThreadExecutor();

        IncrementalStaticRowIterator(Configuration conf) {
            super(conf);
            try {
                rows = new LinkedBlockingQueue<KeySlice>(maxRows);

                indexClient.set_keyspace(indexKeySpace);
                Map<String, String> creds = new HashMap<String, String>();
                creds.put(IAuthenticator.USERNAME_KEY, conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_USERNAME.toString()));
                creds.put(IAuthenticator.PASSWORD_KEY, conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_PASSWORD.toString()));
                AuthenticationRequest authRequest = new AuthenticationRequest(creds);
                indexClient.login(authRequest);

                KsDef ks_def = indexClient.describe_keyspace(indexKeySpace);
                List<String> cfnames = new ArrayList<String>();
                for (CfDef cfd : ks_def.cf_defs)
                    cfnames.add(cfd.name);

                int idx = cfnames.indexOf(indexCfName);

                CfDef cf_def = ks_def.cf_defs.get(idx);
                indexCFComparator = TypeParser.parse(cf_def.comparator_type);
                indexCFSubComparator = cf_def.subcomparator_type == null ? null : TypeParser.parse(cf_def.subcomparator_type);

            } catch (ConfigurationException e) {
                throw new RuntimeException("unable to load sub/comparator", e);
            } catch (TException e) {
                throw new RuntimeException("error communicating via Thrift", e);
            } catch (Exception e) {
                throw new RuntimeException("unable to load keyspace " + indexKeySpace, e);
            }

        }

        private void maybeInit() {
            if (rows.size() <= minRows){
              if(!isCassandraRowsLoaderRunning.get()){
                  //check and start cassandra loader
                  if(isCompletedFetching.get())
                      return;

                  cassandraLoaderService.submit(new CassandraReader());

                  if(rows.size() == 0){
                      while (rows.size() == 0 && !isCompletedFetching.get()) {
                          try {
                              Thread.sleep(1000);
                          } catch (InterruptedException e) {
                          }
                      }
                  }
              }else {
                  // cassandra loader already running, hence wait until the rows getting filled
                  // or check whether it's finished
                  if(rows.size() == 0){
                      while (rows.size() == 0 && !isCompletedFetching.get()) {
                          try {
                              Thread.sleep(1000);
                          } catch (InterruptedException e) {
                          }
                      }

                  }
              }
            }

        }

        private long nextKey(long indexCFRowKey) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(indexCFRowKey);
            calendar.add(Calendar.HOUR, 1);
            return calendar.getTime().getTime();
        }

        private long getColumnNameFromTimeStamp(long timeStamp) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeStamp);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime().getTime();
        }

        private String getIndexCFName() {
            return EVENT_INDEX_TABLE_PREFIX + cfName;
        }

        protected Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> computeNext() {
            maybeInit();

            if (rows == null || rows.size() == 0){
                cassandraLoaderService.shutdownNow();
                return endOfData();
            }

            totalRead++;
            KeySlice ks = rows.poll();
            SortedMap<ByteBuffer, IColumn> map = new TreeMap<ByteBuffer, IColumn>(comparator);
            for (ColumnOrSuperColumn cosc : ks.columns) {
                IColumn column = unthriftify(cosc);
                map.put(column.name(), column);
            }
            return Pair.create(ks.key, map);
        }

        private class CassandraReader implements Runnable {

            public void run() {
                isCassandraRowsLoaderRunning.set(true);
                loadRows();
                isCassandraRowsLoaderRunning.set(false);
            }

            public void loadRows(){
                if(isCompletedFetching.get())
                    return;

                long iterStartTime;
                long aIndexRowKey = 0;

                if(rows ==  null)
                rows = new LinkedBlockingQueue<KeySlice>();

                boolean finishedSplit = false;
                try {
                    do {
                        boolean endRow = false;
                        while (!endRow) {
                            if (isfirstFetch) {
                                // first request
                                iterStartTime = incSplit.getStartColName();
                                aIndexRowKey = incSplit.nextKey();
                                isfirstFetch = false;
                            } else {
                                iterStartTime = lastAccessedIndexColumnName + 1;
                                aIndexRowKey = lastAccessedIndexRowKey;

                                if (incSplit.getEndColName() != -1 && iterStartTime >= incSplit.getEndColName()) {
                                    // reached end of the split
                                    isCompletedFetching.set(true);
                                    return;
                                }
                            }

                            SliceRange range = new SliceRange();


                            range.setStart(ByteBufferUtil.bytes(iterStartTime));
                            if (incSplit.getEndColName() == -1) {
                                range.setFinish(new byte[0]);
                            } else {
                                range.setFinish(ByteBufferUtil.bytes(incSplit.getEndColName()));
                            }

                            range.setCount(maxRowsBlock);
                            //range.setCount(Integer.MAX_VALUE);
                            SlicePredicate predicate = new SlicePredicate();
                            predicate.setSlice_range(range);

                            indexClient.set_keyspace(indexKeySpace);
                            indexClient.login(authRequest);

                            List<ColumnOrSuperColumn> actualKeyList = null;
                            actualKeyList = indexClient.get_slice(ByteBuffer.wrap(String.valueOf(aIndexRowKey).getBytes()),
                                    new ColumnParent(indexCfName), predicate, consistencyLevel);


                            if (actualKeyList == null || actualKeyList.size() == 0) {
                                range = new SliceRange();
                                range.setStart(ByteBufferUtil.bytes(iterStartTime));
                                range.setFinish(new byte[0]);

                                range.setCount(maxRowsBlock);
                                //range.setCount(Integer.MAX_VALUE);
                                predicate = new SlicePredicate();
                                predicate.setSlice_range(range);
                                    actualKeyList = indexClient.get_slice(ByteBuffer.wrap(String.valueOf(aIndexRowKey).getBytes()),
                                            new ColumnParent(indexCfName), predicate, consistencyLevel);

                            }

                            if(actualKeyList == null || actualKeyList.size() == 0 ){
                                aIndexRowKey = incSplit.nextKey();
                                if (aIndexRowKey == -1) {
                                    finishedSplit = true;
                                }
                                endRow = true;
                            } else {
                                ArrayList<ByteBuffer> actualCFRowKeys = new ArrayList<ByteBuffer>();
                                long endSplitTimeMS = -1;
                                if(incSplit.getEndColName() != -1 ){
                                    endSplitTimeMS = Long.parseLong(String.valueOf(incSplit.getEndColName()).substring(0, 13));
                                }

                                long startIterTimeMS = Long.parseLong(String.valueOf(incSplit.getStartColName()).substring(0, 13));

                                for (ColumnOrSuperColumn actualKeyCol : actualKeyList) {
                                     lastAccessedIndexColumnName = actualKeyCol.column.bufferForName().getLong();
                                     long lastIndexColMS = Long.parseLong(String.valueOf(lastAccessedIndexColumnName).substring(0, 13));
                                     if (endSplitTimeMS == -1 || (lastIndexColMS <= endSplitTimeMS && lastIndexColMS >= startIterTimeMS)){
                                        actualCFRowKeys.add(actualKeyCol.column.bufferForValue());
                                     }
                                }
                                if (actualCFRowKeys.size() >0){
                                range = new SliceRange();
                                range.setCount(maxRowsBlock);
                                range.setStart(new byte[0]);
                                range.setFinish(new byte[0]);
                                predicate = new SlicePredicate();
                                predicate.setSlice_range(range);

                                indexClient.set_keyspace(keyspace);
                                indexClient.login(authRequest);


                                Map<ByteBuffer, List<ColumnOrSuperColumn>> actualRows = new HashMap<ByteBuffer, List<ColumnOrSuperColumn>>();
                                boolean isloaded = false;
                                  int iteration = 0;
                                    while (!isloaded && iteration <= maximumCassandraConnectionRetries) {
                                        try {
                                            iteration++;
                                            actualRows = indexClient.
                                                    multiget_slice(actualCFRowKeys, new ColumnParent(cfName), predicate, ConsistencyLevel.ONE);
                                            isloaded = true;
                                            break;
                                        } catch (Throwable throwable) {
                                            logger.warn("Failed to connect to cassandra. Will try to connect "+maximumCassandraConnectionRetries+ "."+throwable.getMessage());
                                            if (iteration >= maximumCassandraConnectionRetries) {
                                                logger.error("Attempted " + maximumCassandraConnectionRetries + " to connect cassandra, " +
                                                        "and terminating the process");
                                                throw new RuntimeException(throwable);
                                            }
                                        }
                                    }

                                    for (ByteBuffer key : actualRows.keySet()) {
                                        KeySlice row = new KeySlice(key, actualRows.get(key));
                                        rows.put(row);
                                        i++;
                                    }
                            }
                        }
                            lastAccessedIndexRowKey = aIndexRowKey;
                        }
                    } while (!finishedSplit && rows.size() < maxRows);
                    //reset to iterate through new batch
                    if(finishedSplit){
                        isCompletedFetching.set(true);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }


    private class StaticRowIterator extends RowIterator {
        protected int i = 0;

        private StaticRowIterator(Configuration conf) {
            super(conf);
        }

        private void maybeInit() {
            // check if we need another batch
            if (rows != null && i < rows.size())
                return;

            String startToken;
            if (totalRead == 0) {
                // first request
                startToken = split.getStartToken();
            } else {
                startToken = partitioner.getTokenFactory().toString(partitioner.getToken(Iterables.getLast(rows).key));
                if (startToken.equals(split.getEndToken())) {
                    // reached end of the split
                    rows = null;
                    return;
                }
            }

            KeyRange keyRange = new KeyRange(batchSize)
                    .setStart_token(startToken)
                    .setEnd_token(split.getEndToken())
                    .setRow_filter(filter);
            try {

                rows = client.get_range_slices(new ColumnParent(cfName), predicate, keyRange, consistencyLevel);

                // nothing new? reached the end
                if (rows.isEmpty()) {
                    rows = null;
                    return;
                }

                // prepare for the next slice to be read
                KeySlice lastRow = rows.get(rows.size() - 1);
                ByteBuffer rowkey = lastRow.key;
                startToken = partitioner.getTokenFactory().toString(partitioner.getToken(rowkey));

                // remove ghosts when fetching all columns
                if (isEmptyPredicate) {
                    Iterator<KeySlice> it = rows.iterator();
                    KeySlice ks;
                    do {
                        ks = it.next();
                        if (ks.getColumnsSize() == 0) {
                            it.remove();
                        }
                    } while (it.hasNext());

                    // all ghosts, spooky
                    if (rows.isEmpty()) {
                        // maybeInit assumes it can get the start-with key from the rows collection, so add back the last
                        rows.add(ks);
                        maybeInit();
                        return;
                    }
                }

                // reset to iterate through this new batch
                i = 0;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> computeNext() {
            maybeInit();
            if (rows == null)
                return endOfData();

            totalRead++;
            KeySlice ks = rows.get(i++);
            SortedMap<ByteBuffer, IColumn> map = new TreeMap<ByteBuffer, IColumn>(comparator);
            for (ColumnOrSuperColumn cosc : ks.columns) {
                IColumn column = unthriftify(cosc);
                map.put(column.name(), column);
            }
            return Pair.create(ks.key, map);
        }
    }


    private class WideRowIterator extends RowIterator {
        private PeekingIterator<Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>>> wideColumns;
        private ByteBuffer lastColumn = ByteBufferUtil.EMPTY_BYTE_BUFFER;

        private WideRowIterator(Configuration conf) {
            super(conf);
        }

        private void maybeInit() {
            if (wideColumns != null && wideColumns.hasNext())
                return;

            KeyRange keyRange;
            ByteBuffer startColumn;
            if (totalRead == 0) {
                String startToken = split.getStartToken();
                keyRange = new KeyRange(batchSize)
                        .setStart_token(startToken)
                        .setEnd_token(split.getEndToken())
                        .setRow_filter(filter);
            } else {
                KeySlice lastRow = Iterables.getLast(rows);
                logger.debug("Starting with last-seen row {}", lastRow.key);
                keyRange = new KeyRange(batchSize)
                        .setStart_key(lastRow.key)
                        .setEnd_token(split.getEndToken())
                        .setRow_filter(filter);
            }

            try {
                rows = client.get_paged_slice(cfName, keyRange, lastColumn, consistencyLevel);
                int n = 0;
                for (KeySlice row : rows)
                    n += row.columns.size();
                logger.debug("read {} columns in {} rows for {} starting with {}",
                        new Object[]{n, rows.size(), keyRange, lastColumn});

                wideColumns = Iterators.peekingIterator(new WideColumnIterator(rows));
                if (wideColumns.hasNext() && wideColumns.peek().right.keySet().iterator().next().equals(lastColumn))
                    wideColumns.next();
                if (!wideColumns.hasNext())
                    rows = null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> computeNext() {
            maybeInit();
            if (rows == null)
                return endOfData();

            totalRead++;
            Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> next = wideColumns.next();
            lastColumn = next.right.values().iterator().next().name();
            return next;
        }

        private class WideColumnIterator extends AbstractIterator<Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>>> {
            private final Iterator<KeySlice> rows;
            private Iterator<ColumnOrSuperColumn> columns;
            public KeySlice currentRow;

            public WideColumnIterator(List<KeySlice> rows) {
                this.rows = rows.iterator();
                if (this.rows.hasNext())
                    nextRow();
                else
                    columns = Iterators.emptyIterator();
            }

            private void nextRow() {
                currentRow = rows.next();
                columns = currentRow.columns.iterator();
            }

            protected Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> computeNext() {
                while (true) {
                    if (columns.hasNext()) {
                        ColumnOrSuperColumn cosc = columns.next();
                        IColumn column = unthriftify(cosc);
                        ImmutableSortedMap<ByteBuffer, IColumn> map = ImmutableSortedMap.of(column.name(), column);
                        return Pair.<ByteBuffer, SortedMap<ByteBuffer, IColumn>>create(currentRow.key, map);
                    }

                    if (!rows.hasNext())
                        return endOfData();

                    nextRow();
                }
            }
        }
    }

    // Because the old Hadoop API wants us to write to the key and value
    // and the new asks for them, we need to copy the output of the new API
    // to the old. Thus, expect a small performance hit.
    // And obviously this wouldn't work for wide rows. But since ColumnFamilyInputFormat
    // and ColumnFamilyRecordReader don't support them, it should be fine for now.
    public boolean next(ByteBuffer key, SortedMap<ByteBuffer, IColumn> value) throws IOException {
        if (this.nextKeyValue()) {
            key.clear();
            key.put(this.getCurrentKey());
            key.rewind();

            value.clear();
            value.putAll(this.getCurrentValue());

            return true;
        }
        return false;
    }

    public ByteBuffer createKey() {
        return ByteBuffer.wrap(new byte[this.keyBufferSize]);
    }

    public SortedMap<ByteBuffer, IColumn> createValue() {
        return new TreeMap<ByteBuffer, IColumn>();
    }

    public long getPos() throws IOException {
        return (long) iter.rowsRead();
    }
}
