package org.apache.hadoop.hive.cassandra.input;

/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */


import com.google.common.collect.AbstractIterator;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.cassandra.serde.AbstractColumnSerDe;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

public class ColumnFamilyWideRowRecordReader extends ColumnFamilyRecordReader {
  public static final int CASSANDRA_HADOOP_MAX_KEY_SIZE_DEFAULT = 8192;
  static final Log LOG = LogFactory.getLog(ColumnFamilyWideRowRecordReader.class);

  private ColumnFamilySplit split;
  private IncrementalColumnFamilySplit incSplit;
  private WideRowIterator iter;
  private Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> currentRow;
  private SlicePredicate predicate;
  private int totalRowCount; // total number of rows to fetch
  private int batchRowCount; // fetch this many per batch
  private int rowPageSize; // total number of columns to read
  private ByteBuffer startSlicePredicate;
  private String cfName;
  private ConsistencyLevel consistencyLevel;
  private int keyBufferSize = 8192;
  private boolean isCFIncremental;
  private String indexCfName;
  private String keyspace;
  private String indexKeySpace;
  private TSocket socket;
  private TSocket indexClientSocket;
  private Cassandra.Client client;
  private Cassandra.Client indexClient;
  private AuthenticationRequest authRequest;

  public ColumnFamilyWideRowRecordReader() {
    this(ColumnFamilyWideRowRecordReader.CASSANDRA_HADOOP_MAX_KEY_SIZE_DEFAULT);
  }

  public ColumnFamilyWideRowRecordReader(int keyBufferSize) {
    super();
    this.keyBufferSize = keyBufferSize;
  }

  @Override
  public void close() {
    if (socket != null && socket.isOpen()) {
      socket.close();
      socket = null;
      client = null;
    }
  }

  @Override
  public ByteBuffer getCurrentKey() {
    return currentRow.left;
  }

  @Override
  public SortedMap<ByteBuffer, IColumn> getCurrentValue() {
    return currentRow.right;
  }

  @Override
  public float getProgress() {
    // the progress is likely to be reported slightly off the actual but close enough
    return iter.rowsRead() > totalRowCount ? 1.0f : ((float) iter.rowsRead()) / totalRowCount;
  }

  static boolean isSliceRangePredicate(SlicePredicate predicate) {
    if (predicate == null) {
      return false;
    }

    if (predicate.isSetColumn_names() && predicate.getSlice_range() == null) {
      return false;
    }

    if (predicate.getSlice_range() == null) {
      return false;
    }

    byte[] start = predicate.getSlice_range().getStart();
    byte[] finish = predicate.getSlice_range().getFinish();
    if (start != null && finish != null) {
      return true;
    }

    return false;
  }

  @Override
  public void initialize(InputSplit split, TaskAttemptContext context) throws IOException {
    Configuration conf = context.getConfiguration();
    predicate = ConfigHelper.getInputSlicePredicate(conf);

    if (!isSliceRangePredicate(predicate)) {
      throw new AssertionError("WideRowsRequire a slice range");
    }

    totalRowCount = ConfigHelper.getInputSplitSize(conf);
    LOG.info("total rows = " + totalRowCount);
    batchRowCount = 1;
    rowPageSize = predicate.getSlice_range().getCount();
    startSlicePredicate = predicate.getSlice_range().start;
    cfName = ConfigHelper.getInputColumnFamily(conf);
    consistencyLevel = ConsistencyLevel.valueOf(ConfigHelper.getReadConsistencyLevel(conf));

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
              indexCfName = IncrementalWideRowIterator.EVENT_INDEX_TABLE_PREFIX + cfName;
              indexKeySpace = conf.get(HiveConf.ConfVars.HIVE_INCREMENTAL_PROCESS_KEYSPACE.toString());
              incSplit = (IncrementalColumnFamilySplit) split;

          } else {
              this.split = (ColumnFamilySplit) split;
          }
      } else {
          this.split = (ColumnFamilySplit) split;
      }


      try {
      // only need to connect once
      if (socket != null && socket.isOpen()) {
        return;
      }

          String trustStore = conf.get(AbstractColumnSerDe.SSL_TRUSTSTORE, null);
          String trustStorePassword = conf.get(AbstractColumnSerDe.SSL_TRUSTSTORE_PASSWORD, null);

          TSSLTransportFactory.TSSLTransportParameters
                  tsslTransportParameters = new TSSLTransportFactory.TSSLTransportParameters();

          if ((trustStore != null && trustStore.isEmpty()) &&
                  (trustStorePassword != null && !trustStorePassword.isEmpty())) {
              tsslTransportParameters.setTrustStore(trustStore, trustStorePassword);
          }

      // create connection using thrift
      String location = getLocation(conf);
      //socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
          if ((trustStore != null && trustStore.isEmpty()) && (trustStorePassword != null && !trustStorePassword.isEmpty())) {
              socket = TSSLTransportFactory.getClientSocket(
                      location, ConfigHelper.getInputRpcPort(conf),
                      AbstractColumnSerDe.SSL_TIME_OUT_VALUE, tsslTransportParameters);
          } else {
              socket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
              socket.open();
          }
      TBinaryProtocol binaryProtocol = new TBinaryProtocol(new TFramedTransport(socket), true, true);
      client = new Cassandra.Client(binaryProtocol);
      //socket.open();

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

              // create connection using thrift
              // indexClientSocket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
              if ((trustStore != null && !trustStore.isEmpty()) && (trustStorePassword != null && !trustStorePassword.isEmpty())) {
                  indexClientSocket = TSSLTransportFactory.getClientSocket(
                          location, ConfigHelper.getInputRpcPort(conf),
                          AbstractColumnSerDe.SSL_TIME_OUT_VALUE, tsslTransportParameters);
              } else {
                  indexClientSocket = new TSocket(location, ConfigHelper.getInputRpcPort(conf));
              }
              TBinaryProtocol indexBinaryProtocol = new TBinaryProtocol(new TFramedTransport(indexClientSocket), true, true);
              indexClient = new Cassandra.Client(indexBinaryProtocol);

              if ((trustStore == null || trustStore.isEmpty()) && (trustStorePassword == null || trustStorePassword.isEmpty())) {
                  indexClientSocket.open();
              }

              indexClient.set_keyspace(indexKeySpace);

              Map<String, String> creds = new HashMap<String, String>();
              creds.put(IAuthenticator.USERNAME_KEY, ConfigHelper.getInputKeyspaceUserName(conf));
              creds.put(IAuthenticator.PASSWORD_KEY, ConfigHelper.getInputKeyspaceUserName(conf));
              AuthenticationRequest authRequest = new AuthenticationRequest(creds);
              indexClient.login(authRequest);
          }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    if(isCFIncremental) iter = new IncrementalWideRowIterator();
  }

  @Override
  public boolean nextKeyValue() throws IOException {
    if (!iter.hasNext()) {
      return false;
    }
    currentRow = iter.next();
    return true;
  }

  // we don't use endpointsnitch since we are trying to support hadoop nodes that are
  // not necessarily on Cassandra machines, too. This should be adequate for single-DC clusters, at
  // least.
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
                        return location;
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

                return location;
            } catch (Exception ignored) {
                //ignore. move to next host in the list
                   if (LOG.isDebugEnabled())
                    LOG.debug("Host " + location + " seems to be down. Trying next hosts in " +
                            "the list..", ignored);
            }
        }

        return split.getLocations()[0];
    }

    private class IncrementalWideRowIterator extends
      WideRowIterator {
    private int columnsRead = 0;
    private ByteBuffer prevStartSlice = null;
    private int totalRead = 0;

    private final AbstractType comparator;
    private final AbstractType subComparator;
    private final IPartitioner partitioner;

    private int numberOdRowsLoaded;
    private long lastAccessedIndexColumnName;
    private long lastAccessedIndexRowKey;

    private ArrayList<ByteBuffer> actualCFRowKeys;
    private int maximumRowsLoaded = 100000;
    private int iteration;
    private List<ColumnOrSuperColumn> columnList;
    private  ByteBuffer currentRowKey;


    private static final String EVENT_INDEX_TABLE_PREFIX = "event_index_";

    private IncrementalWideRowIterator() {
      try {
        partitioner = FBUtilities.newPartitioner(client.describe_partitioner());

          KsDef ks_def = client.describe_keyspace(keyspace);
          CfDef cf_def = findCfDef(ks_def, cfName);
          if (cf_def == null) {
              throw new RuntimeException("ColumnFamily named " + cfName + " wasn't found in keyspace " + ks_def.name);
          }

          comparator = TypeParser.parse(cf_def.comparator_type);
          subComparator = cf_def.subcomparator_type == null ? null : TypeParser
                  .parse(cf_def.subcomparator_type);

          KsDef indexKsDef = client.describe_keyspace(indexKeySpace);
          CfDef indexCFDef = findCfDef(indexKsDef, indexCfName);
          if(indexCFDef == null){
              throw new RuntimeException("Index ColumnFamily named " + indexCfName + " wasn't found in keyspace " +
                      indexKsDef.name);
          }

      } catch (ConfigurationException e) {
        throw new RuntimeException("unable to load sub/comparator", e);
      } catch (TException e) {
        throw new RuntimeException("error communicating via Thrift", e);
      } catch (Exception e) {
        throw new RuntimeException("unable to load keyspace " + keyspace, e);
      }
    }

    private CfDef findCfDef(KsDef ks_def, String cfName)
    {
        for (CfDef cfDef : ks_def.cf_defs)
        {
            if (cfDef.name.equals(cfName)) {
              return cfDef;
            }
        }

       return null;
    }

        private ByteBuffer getNextRowKey() {
            if (actualCFRowKeys != null && iteration < actualCFRowKeys.size()) {
                return actualCFRowKeys.get(iteration);
            } else {

                long iterStartTime;
                long aIndexRowKey = 0;


                numberOdRowsLoaded = 0;
                actualCFRowKeys = new ArrayList<ByteBuffer>();
                boolean finishedSplit = false;
                try {
                    do {
                        boolean endRow = false;
                        while (!endRow) {
                            if (totalRead == 0) {
                                // first request
                                iterStartTime = incSplit.getStartColName();
                                aIndexRowKey = incSplit.nextKey();
                            } else {
                                iterStartTime = lastAccessedIndexColumnName + 1;
                                aIndexRowKey = lastAccessedIndexRowKey;

                                if (incSplit.getEndColName() != -1 && iterStartTime >= incSplit.getEndColName()) {
                                    // reached end of the split
                                    iteration = 0;
                                    return null;
                                }
                            }

                            SliceRange range = new SliceRange();


                            range.setStart(ByteBufferUtil.bytes(iterStartTime));
                            if (incSplit.getEndColName() == -1) {
                                range.setFinish(new byte[0]);
                            } else {
                                range.setFinish(ByteBufferUtil.bytes(incSplit.getEndColName()));
                            }

                            range.setCount(Integer.MAX_VALUE);
                            SlicePredicate predicate = new SlicePredicate();
                            predicate.setSlice_range(range);

                            indexClient.set_keyspace(indexKeySpace);
                            indexClient.login(authRequest);

                            List<ColumnOrSuperColumn> actualKeyList = indexClient.get_slice(ByteBuffer.wrap(String.valueOf(aIndexRowKey).getBytes()),
                                    new ColumnParent(indexCfName), predicate, consistencyLevel);


                            if (actualKeyList == null || actualKeyList.size() == 0) {
                                aIndexRowKey = incSplit.nextKey();
                                if (aIndexRowKey == -1) {
                                    finishedSplit = true;
                                }
                                endRow = true;
                            } else {
                                actualCFRowKeys = new ArrayList<ByteBuffer>();
                                for (ColumnOrSuperColumn actualKeyCol : actualKeyList) {
                                    if (numberOdRowsLoaded < maximumRowsLoaded) {
                                        actualCFRowKeys.add(actualKeyCol.column.bufferForValue());
                                        numberOdRowsLoaded++;
                                        totalRead++;
                                        lastAccessedIndexColumnName = actualKeyCol.column.bufferForName().getLong();
                                    } else {
                                        endRow = true;
                                        break;
                                    }
                                }
                            }
                            lastAccessedIndexRowKey = aIndexRowKey;
                        }
                    } while (!finishedSplit && numberOdRowsLoaded < maximumRowsLoaded);
                    //reset to iterate through new batch
                    iteration = 0;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                if (null != actualCFRowKeys && actualCFRowKeys.size() > 0) {
                    return actualCFRowKeys.get(iteration);
                }
                return null;
            }
        }


        private void processRow(){
               // check if we need another row
               if (columnsRead < rowPageSize) {
                   columnsRead = 0;
                   predicate.getSlice_range().setStart(startSlicePredicate);
                   prevStartSlice = null;
                   currentRowKey = getNextRowKey();
                   iteration++;
               }

                 //reached end..
                 if (null == currentRowKey) {
                     columnList = null;
                     return;
                 }


             try {
                client.login(authRequest);
                client.set_keyspace(keyspace);
                columnList = client.get_slice(currentRowKey, new ColumnParent(cfName), predicate, consistencyLevel);


               //detect infinite loop
               if (prevStartSlice != null && ByteBufferUtil.compareUnsigned(prevStartSlice, predicate.slice_range.start) == 0) {
                   columnList = null;
                   return;
               }

               // prepare for the next slice to be read

               if ( columnList !=  null &&columnList.size()> 0) {

                 ColumnOrSuperColumn cosc = columnList.get(columnList.size()-1);

                 prevStartSlice = predicate.slice_range.start;

                 //prepare next slice
                 if (cosc.column != null) {
                   predicate.slice_range.start = cosc.column.name;
                 }

                 if (cosc.super_column != null) {
                   predicate.slice_range.start = cosc.super_column.name;
                 }

                 if (cosc.counter_column != null) {
                   predicate.slice_range.start = cosc.counter_column.name;
                 }

                 if (cosc.counter_super_column != null) {
                   predicate.slice_range.start = cosc.counter_super_column.name;
                 }

                columnsRead = columnList.size()-1;
                 //If we've hit the max columns then rm the last column
                 //to make sure we don't know where to start next without overlap
                 if (columnList.size() == rowPageSize) {
                   columnList.remove(columnsRead - 1);
                 }
               }
             } catch (Exception e) {
               throw new RuntimeException(e);
             }
           }


    /**
     * @return total number of rows read by this record reader
     */
    public int rowsRead() {
      return totalRead;
    }

    @Override
    protected Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> computeNext() {
        processRow();

        SortedMap<ByteBuffer, IColumn> map = new TreeMap<ByteBuffer, IColumn>(comparator);
        if (null != columnList) {
            for (ColumnOrSuperColumn cosc : columnList) {
                IColumn column = unthriftify(cosc);
                map.put(column.name(), column);
            }
            return Pair.create(currentRowKey, map);
        } else {
            return endOfData();
        }
    }

    private IColumn unthriftify(ColumnOrSuperColumn cosc) {
      if (cosc.counter_column != null) {
        return unthriftifyCounter(cosc.counter_column);
      }
      if (cosc.counter_super_column != null) {
        return unthriftifySuperCounter(cosc.counter_super_column);
      }
      if (cosc.super_column != null) {
        return unthriftifySuper(cosc.super_column);
      }
      assert cosc.column != null;
      return unthriftifySimple(cosc.column);
    }

    private IColumn unthriftifySuper(SuperColumn super_column) {
      org.apache.cassandra.db.SuperColumn sc = new org.apache.cassandra.db.SuperColumn(
          super_column.name, subComparator);
      for (Column column : super_column.columns) {
        sc.addColumn(unthriftifySimple(column));
      }
      return sc;
    }

    private IColumn unthriftifySimple(Column column) {
      return new org.apache.cassandra.db.Column(column.name, column.value, column.timestamp);
    }

    private IColumn unthriftifyCounter(CounterColumn column) {
      // CounterColumns read the nodeID from the System table, so need the StorageService running
      // and access
      // to cassandra.yaml. To avoid a Hadoop needing access to yaml return a regular Column.
      return new org.apache.cassandra.db.Column(column.name, ByteBufferUtil.bytes(column.value), 0);
    }

    private IColumn unthriftifySuperCounter(CounterSuperColumn superColumn) {
      org.apache.cassandra.db.SuperColumn sc = new org.apache.cassandra.db.SuperColumn(
          superColumn.name, subComparator);
      for (CounterColumn column : superColumn.columns) {
        sc.addColumn(unthriftifyCounter(column));
      }
      return sc;
    }
  }


  private class WideRowIterator extends
      AbstractIterator<Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>>> {
    private List<KeySlice> rows;
    private String startToken;
    private int columnsRead = 0;
    private ByteBuffer prevStartSlice = null;
    private int totalRead = 0;
    private final AbstractType comparator;
    private final AbstractType subComparator;
    private final IPartitioner partitioner;

    private WideRowIterator() {
      try {
        partitioner = FBUtilities.newPartitioner(client.describe_partitioner());

        KsDef ks_def = client.describe_keyspace(keyspace);
        CfDef cf_def = findCfDef(ks_def, cfName);
        if (cf_def == null) {
          throw new RuntimeException("ColumnFamily named " + cfName + " wasn't found in keyspace " + ks_def.name);
        }

        comparator = TypeParser.parse(cf_def.comparator_type);
        subComparator = cf_def.subcomparator_type == null ? null : TypeParser
            .parse(cf_def.subcomparator_type);
      } catch (ConfigurationException e) {
        throw new RuntimeException("unable to load sub/comparator", e);
      } catch (TException e) {
        throw new RuntimeException("error communicating via Thrift", e);
      } catch (Exception e) {
        throw new RuntimeException("unable to load keyspace " + keyspace, e);
      }
    }

    private CfDef findCfDef(KsDef ks_def, String cfName)
    {
        for (CfDef cfDef : ks_def.cf_defs)
        {
            if (cfDef.name.equals(cfName)) {
              return cfDef;
            }
        }

       return null;
    }

    protected void maybeInit() {
      // check if we need another row
      if (rows != null && columnsRead < rowPageSize) {
        columnsRead = 0;
        startToken = partitioner.getTokenFactory().toString(partitioner.getToken(rows.get(0).key));
        predicate.getSlice_range().setStart(startSlicePredicate);
        rows = null;
        prevStartSlice = null;
        totalRead++;
      }

      if (startToken == null) {
        startToken = split.getStartToken();
      } else if (startToken.equals(split.getEndToken()) && rows == null) {
        // reached end of the split
        return;
      }

      KeyRange keyRange = new KeyRange(batchRowCount)
                                .setStart_token(startToken)
                                .setEnd_token(split.getEndToken());
      try {
        rows = client.get_range_slices(new ColumnParent(cfName),
                                               predicate,
                                               keyRange,
                                               consistencyLevel);

        // nothing new? reached the end
        if (rows.isEmpty()) {
          rows = null;
          return;
        }

        //detect infinite loop
        if (prevStartSlice != null && ByteBufferUtil.compareUnsigned(prevStartSlice, predicate.slice_range.start) == 0) {
            rows = null;
            return;
        }

        // prepare for the next slice to be read
        KeySlice row = rows.get(0);

        if (row.getColumnsSize() > 0) {

          ColumnOrSuperColumn cosc = row.getColumns().get(row.getColumnsSize() - 1);

          prevStartSlice = predicate.slice_range.start;

          //prepare next slice
          if (cosc.column != null) {
            predicate.slice_range.start = cosc.column.name;
          }

          if (cosc.super_column != null) {
            predicate.slice_range.start = cosc.super_column.name;
          }

          if (cosc.counter_column != null) {
            predicate.slice_range.start = cosc.counter_column.name;
          }

          if (cosc.counter_super_column != null) {
            predicate.slice_range.start = cosc.counter_super_column.name;
          }

          columnsRead = row.getColumnsSize();

          //If we've hit the max columns then rm the last column
          //to make sure we don't know where to start next without overlap
          if (columnsRead == rowPageSize) {
            row.getColumns().remove(columnsRead - 1);
          }
        } 
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }



    /**
     * @return total number of rows read by this record reader
     */
    public int rowsRead() {
      return totalRead;
    }

    @Override
    protected Pair<ByteBuffer, SortedMap<ByteBuffer, IColumn>> computeNext() {
      maybeInit();
      if (rows == null) {
        return endOfData();
      }

      KeySlice ks = rows.get(0);
      SortedMap<ByteBuffer, IColumn> map = new TreeMap<ByteBuffer, IColumn>(comparator);
      for (ColumnOrSuperColumn cosc : ks.columns) {
        IColumn column = unthriftify(cosc);
        map.put(column.name(), column);
      }
      return Pair.create(ks.key, map);
    }

    private IColumn unthriftify(ColumnOrSuperColumn cosc) {
      if (cosc.counter_column != null) {
        return unthriftifyCounter(cosc.counter_column);
      }
      if (cosc.counter_super_column != null) {
        return unthriftifySuperCounter(cosc.counter_super_column);
      }
      if (cosc.super_column != null) {
        return unthriftifySuper(cosc.super_column);
      }
      assert cosc.column != null;
      return unthriftifySimple(cosc.column);
    }

    private IColumn unthriftifySuper(SuperColumn super_column) {
      org.apache.cassandra.db.SuperColumn sc = new org.apache.cassandra.db.SuperColumn(
          super_column.name, subComparator);
      for (Column column : super_column.columns) {
        sc.addColumn(unthriftifySimple(column));
      }
      return sc;
    }

    private IColumn unthriftifySimple(Column column) {
      return new org.apache.cassandra.db.Column(column.name, column.value, column.timestamp);
    }

    private IColumn unthriftifyCounter(CounterColumn column) {
      // CounterColumns read the nodeID from the System table, so need the StorageService running
      // and access
      // to cassandra.yaml. To avoid a Hadoop needing access to yaml return a regular Column.
      return new org.apache.cassandra.db.Column(column.name, ByteBufferUtil.bytes(column.value), 0);
    }

    private IColumn unthriftifySuperCounter(CounterSuperColumn superColumn) {
      org.apache.cassandra.db.SuperColumn sc = new org.apache.cassandra.db.SuperColumn(
          superColumn.name, subComparator);
      for (CounterColumn column : superColumn.columns) {
        sc.addColumn(unthriftifyCounter(column));
      }
      return sc;
    }
  }


  // Because the old Hadoop API wants us to write to the key and value
  // and the new asks for them, we need to copy the output of the new API
  // to the old. Thus, expect a small performance hit.
  // And obviously this wouldn't work for wide rows. But since ColumnFamilyInputFormat
  // and ColumnFamilyRecordReader don't support them, it should be fine for now.
  @Override
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

  @Override
  public ByteBuffer createKey() {
    return ByteBuffer.wrap(new byte[this.keyBufferSize]);
  }

  @Override
  public SortedMap<ByteBuffer, IColumn> createValue() {
    return new TreeMap<ByteBuffer, IColumn>();
  }

  @Override
  public long getPos() throws IOException {
    return (long) iter.rowsRead();
  }
}
