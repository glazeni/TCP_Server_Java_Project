package Server;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

/**
 *
 * @author glazen
 */
public class ServerUI extends javax.swing.JFrame implements ActionListener {

    private InetAddress addr = null;
    private TCPClient tcpClient = null;
    private TCPServer tcpServer = null;
    private static TimeSeries series = null;
    //Timer to refresh graph after every second
    private static javax.swing.Timer timer = null;
    private boolean isNagleDisable;
    private boolean isIperfSettings;

    public ServerUI() {
        initComponents();
        timer = new javax.swing.Timer(1000, this);
        timer.start();
        setInterfaceEnable(false);
        DynamicLineAndTimeSeriesChart("Data Measurement");

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jActiveButton = new javax.swing.JToggleButton();
        jRadioServerButton = new javax.swing.JRadioButton();
        jRadioClientButton = new javax.swing.JRadioButton();
        jTextIPclient = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jTextIPserver = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextPortClient = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTextPortServer = new javax.swing.JTextField();
        jBeginMeasurement = new javax.swing.JButton();
        jGraphPanel = new javax.swing.JPanel();
        jTCPpanel = new javax.swing.JPanel();
        jSpinner3 = new javax.swing.JSpinner();
        jSpinner4 = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jSpinner5 = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jSpinner6 = new javax.swing.JSpinner();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextLogger = new javax.swing.JTextArea();
        jLabel11 = new javax.swing.JLabel();
        jCheckBoxNagle = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jActiveButton.setText("Active");
        jActiveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jActiveButtonActionPerformed(evt);
            }
        });

        jRadioServerButton.setText("Server");
        jRadioServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioServerButtonActionPerformed(evt);
            }
        });

        jRadioClientButton.setText("Client");
        jRadioClientButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioClientButtonActionPerformed(evt);
            }
        });

        jTextIPclient.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextIPclientActionPerformed(evt);
            }
        });

        jLabel1.setText("IP");

        jTextIPserver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextIPserverActionPerformed(evt);
            }
        });

        jLabel2.setText("IP");

        jTextPortClient.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextPortClientActionPerformed(evt);
            }
        });

        jLabel3.setText("Port");

        jLabel4.setText("Port");

        jTextPortServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextPortServerActionPerformed(evt);
            }
        });

        jBeginMeasurement.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        jBeginMeasurement.setText("Begin Measurement");
        jBeginMeasurement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBeginMeasurementActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jGraphPanelLayout = new javax.swing.GroupLayout(jGraphPanel);
        jGraphPanel.setLayout(jGraphPanelLayout);
        jGraphPanelLayout.setHorizontalGroup(
            jGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 491, Short.MAX_VALUE)
        );
        jGraphPanelLayout.setVerticalGroup(
            jGraphPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 293, Short.MAX_VALUE)
        );

        jTCPpanel.setBackground(new java.awt.Color(230, 230, 230));
        jTCPpanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel7.setText("TCP Receive Window");

        jLabel8.setText("TCP Send Window");

        jLabel9.setFont(new java.awt.Font("Lucida Grande", 1, 18)); // NOI18N
        jLabel9.setText("TCP Properties");

        jLabel10.setText("Socket Timeout");

        jLabel6.setText("Buffers Size");

        javax.swing.GroupLayout jTCPpanelLayout = new javax.swing.GroupLayout(jTCPpanel);
        jTCPpanel.setLayout(jTCPpanelLayout);
        jTCPpanelLayout.setHorizontalGroup(
            jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jTCPpanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jTCPpanelLayout.createSequentialGroup()
                        .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jTCPpanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel10)
                                .addGap(42, 42, 42))
                            .addGroup(jTCPpanelLayout.createSequentialGroup()
                                .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel8))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jSpinner6, javax.swing.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
                            .addComponent(jSpinner5)
                            .addComponent(jSpinner4)
                            .addComponent(jSpinner3)))
                    .addGroup(jTCPpanelLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jTCPpanelLayout.setVerticalGroup(
            jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jTCPpanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel9)
                .addGap(18, 18, 18)
                .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jSpinner3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jSpinner4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jSpinner5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jTCPpanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jSpinner6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTextLogger.setColumns(20);
        jTextLogger.setRows(5);
        jScrollPane1.setViewportView(jTextLogger);

        jLabel11.setFont(new java.awt.Font("Lucida Grande", 1, 18)); // NOI18N
        jLabel11.setText("Logger");

        jCheckBoxNagle.setText("Disable Nagle Algorithm");
        jCheckBoxNagle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxNagleActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jRadioClientButton)
                                    .addComponent(jRadioServerButton))
                                .addGap(25, 25, 25)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextIPclient, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextIPserver, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel3)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextPortClient, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextPortServer, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addComponent(jBeginMeasurement, javax.swing.GroupLayout.PREFERRED_SIZE, 184, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jActiveButton))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 734, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addComponent(jCheckBoxNagle))
                            .addComponent(jTCPpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(14, 14, 14))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jBeginMeasurement, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jActiveButton)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jRadioClientButton)
                                .addComponent(jTextIPclient, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel1)
                                .addComponent(jLabel3))
                            .addComponent(jTextPortClient, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextPortServer, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel4))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jRadioServerButton)
                                .addComponent(jTextIPserver, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2)))))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jGraphPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxNagle)
                        .addGap(28, 28, 28)
                        .addComponent(jTCPpanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel11)
                        .addGap(4, 4, 4)))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents

//     *************************************
//     ************* METHODS *************** 
//     ************************************
    public synchronized void Log(String s) {
        try {
            jTextLogger.append(s+"\n");
            System.out.print(s);
        } catch (Exception e) {
            System.err.println("Error in Log: " + e + "\n");
        }
    }

    //Enable and disable Interface components
    private void setInterfaceEnable(boolean enabled) {
        if (enabled) {
            jBeginMeasurement.setEnabled(true);
            jRadioClientButton.setEnabled(true);
            jRadioServerButton.setEnabled(true);
            jTextIPclient.setEnabled(true);
            jTextIPserver.setEnabled(true);
            jTextPortClient.setEnabled(true);
            jTextPortServer.setEnabled(true);
            jTextLogger.setEnabled(true);
            jSpinner3.setEnabled(true);
            jSpinner4.setEnabled(true);
            jSpinner5.setEnabled(true);
            jSpinner6.setEnabled(true);
            jSpinner6.setEnabled(true);
            jTCPpanel.setEnabled(true);
            jGraphPanel.setEnabled(true);
            jCheckBoxNagle.setEnabled(true);
        } else {
            jBeginMeasurement.setEnabled(false);
            jRadioClientButton.setEnabled(false);
            jRadioServerButton.setEnabled(false);
            jTextIPclient.setEnabled(false);
            jTextIPserver.setEnabled(false);
            jTextPortClient.setEnabled(false);
            jTextPortServer.setEnabled(false);
            jTextLogger.setEnabled(false);
            jSpinner3.setEnabled(false);
            jSpinner4.setEnabled(false);
            jSpinner5.setEnabled(false);
            jSpinner6.setEnabled(false);
            jSpinner6.setEnabled(false);
            jTCPpanel.setEnabled(false);
            jGraphPanel.setEnabled(false);
            jCheckBoxNagle.setEnabled(false);
        }
        jGraphPanel.updateUI();
    }

    //Update graph in real time
    private void DynamicLineAndTimeSeriesChart(final String seriesString) {

        series = new TimeSeries(seriesString, Second.class);
        final TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        JFreeChart chart = createChart(dataset);
        //Created Chartpanel for chart area
        ChartPanel chartPanel = new ChartPanel(chart);
        //Sets the size of whole window (JPanel)
        timer.setInitialDelay(1000);
        chartPanel.setPreferredSize(new java.awt.Dimension(jGraphPanel.getWidth(), jGraphPanel.getHeight()));
        chartPanel.setDomainZoomable(true);
        //jPanel to show graph on the screen
        jGraphPanel.setLayout(new BorderLayout());
        jGraphPanel.setBackground(Color.red);
        //Added chartpanel to main panel
        jGraphPanel.add(chartPanel, BorderLayout.NORTH);
        //Puts the whole content on a Frame
        //setContentPane(jPanel1);
    }

    private JFreeChart createChart(final XYDataset dataset) {
        final JFreeChart result = ChartFactory.createTimeSeriesChart(
                "Bandwidth Estimator",
                "Time/s",
                "Bandwidth",
                dataset,
                true,
                true,
                false
        );

        final XYPlot plot = result.getXYPlot();
        Color plotColor = new Color(245, 245, 245);
        plot.setBackgroundPaint(plotColor);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.lightGray);
        ValueAxis xaxis = plot.getDomainAxis();
        //xaxis.setAutoRange(true);
        xaxis.setFixedAutoRange(5000.0);
        //xaxis.setVerticalTickLabels(true);
        ValueAxis yaxis = plot.getRangeAxis();
        yaxis.setAutoRange(true);
        //yaxis.setRange(0.0, 300.0);
        return result;
    }

    //Generates an random entry for a particular call made by time for every second 
    public void actionPerformed(final ActionEvent e) {
        try {
            //Create a Second object to solve duplicate time series
            series.addOrUpdate(new Second(new Date(), TimeZone.getDefault()), RTInputStream.bytesGraph);
            //System.err.println("RTin.byteCnt="+RTInputStream.byteCnt);
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally{
            RTInputStream.bytesGraph=0;
        }
    }

//     **********************************************
//     ******************* INTERFACE ****************
//     **********************************************

    private void jActiveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jActiveButtonActionPerformed
        try {
            if (jActiveButton.isSelected()) {
                setInterfaceEnable(true);
                jRadioServerButton.setSelected(true);
                jTextIPclient.setEnabled(false);
                jTextPortClient.setEnabled(false);
                //Create TCPServer Instance
                tcpServer = new TCPServer();
                tcpServer.start();
                addr = InetAddress.getLocalHost();
                jTextIPserver.setText(Constants.SERVER_IP);
                jTextPortServer.setText(String.valueOf(Constants.SERVERPORT));;
                Log("Server started! \n");

                //Spinners Listeners
               
                //Packet Size
                jSpinner3.setValue((Integer) Constants.BUFFERSIZE);
                jSpinner3.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        Constants.BUFFERSIZE = (Integer) jSpinner3.getValue();
                    }
                });
                //Socket rcv Buffer
                jSpinner4.setValue((Integer) Constants.SOCKET_RCVBUF);
                jSpinner4.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        Constants.SOCKET_RCVBUF = (Integer) jSpinner4.getValue();
                    }
                });
                //Socket snd Buffer
                jSpinner5.setValue((Integer) Constants.SOCKET_SNDBUF);
                jSpinner5.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        Constants.SOCKET_SNDBUF = (Integer) jSpinner5.getValue();
                    }
                });
                //Socket Timeout
                jSpinner6.setValue((Integer) Constants.SO_TIMEOUT);
                jSpinner6.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        Constants.SO_TIMEOUT = (Integer) jSpinner6.getValue();
                    }
                });
            } else {
                setInterfaceEnable(false);
                jRadioClientButton.setSelected(false);
                jRadioServerButton.setSelected(false);
                //tcpServ.interrupt();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }//GEN-LAST:event_jActiveButtonActionPerformed

    private void jTextIPclientActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextIPclientActionPerformed
        //Done in jRadioClientButton()
    }//GEN-LAST:event_jTextIPclientActionPerformed

    private void jTextIPserverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextIPserverActionPerformed
        //Done in jRadioServerButton()
    }//GEN-LAST:event_jTextIPserverActionPerformed

    private void jTextPortClientActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextPortClientActionPerformed
        //Done in jRadioClientButton()
    }//GEN-LAST:event_jTextPortClientActionPerformed

    private void jTextPortServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextPortServerActionPerformed
        //Done in jRadioServerButton()    
    }//GEN-LAST:event_jTextPortServerActionPerformed

    private void jBeginMeasurementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBeginMeasurementActionPerformed
        try {
            //CLIENT MODE
            if (jRadioClientButton.isSelected()) {
                tcpClient = new TCPClient(isNagleDisable);
                tcpClient.start();
            } else {
                //SERVER MODE
                Log("Server listening on Port: " + Constants.SERVERPORT + "\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            jTextPortServer.setText(String.valueOf(Constants.SERVERPORT));
            jTextPortClient.setText(String.valueOf(Constants.SERVERPORT));
        }
    }//GEN-LAST:event_jBeginMeasurementActionPerformed

    private void jRadioClientButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioClientButtonActionPerformed
        try {
            jTextIPclient.setEnabled(true);
            jTextPortClient.setEnabled(true);
            jRadioClientButton.setSelected(true);
            jRadioServerButton.setSelected(false);
            addr = InetAddress.getLocalHost();
            jTextIPclient.setText(addr.getHostAddress());
            jTextPortClient.setText(String.valueOf(Constants.SERVERPORT));
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            jTextIPserver.setEnabled(false);
            jTextPortServer.setEnabled(false);
        }
    }//GEN-LAST:event_jRadioClientButtonActionPerformed

    private void jRadioServerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioServerButtonActionPerformed
        try {
            jTextIPserver.setEnabled(true);
            jTextPortServer.setEnabled(true);
            jRadioServerButton.setSelected(true);
            jRadioClientButton.setSelected(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            jTextIPclient.setEnabled(false);
            jTextPortClient.setEnabled(false);
        }
    }//GEN-LAST:event_jRadioServerButtonActionPerformed

    private void jCheckBoxNagleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxNagleActionPerformed
        isNagleDisable = jCheckBoxNagle.isSelected();
        if (isNagleDisable) {
            Log("Nagle OFF");
        } else {
            Log("Nagle ON");
        }
    }//GEN-LAST:event_jCheckBoxNagleActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());

                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ServerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ServerUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton jActiveButton;
    private javax.swing.JButton jBeginMeasurement;
    private javax.swing.JCheckBox jCheckBoxNagle;
    private javax.swing.JPanel jGraphPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JRadioButton jRadioClientButton;
    private javax.swing.JRadioButton jRadioServerButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jSpinner3;
    private javax.swing.JSpinner jSpinner4;
    private javax.swing.JSpinner jSpinner5;
    private javax.swing.JSpinner jSpinner6;
    private javax.swing.JPanel jTCPpanel;
    private javax.swing.JTextField jTextIPclient;
    private javax.swing.JTextField jTextIPserver;
    private javax.swing.JTextArea jTextLogger;
    private javax.swing.JTextField jTextPortClient;
    private javax.swing.JTextField jTextPortServer;
    // End of variables declaration//GEN-END:variables
}
