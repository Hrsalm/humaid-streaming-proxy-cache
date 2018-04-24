/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientvideorunner;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import java.io.File;  
import java.io.FileNotFoundException;
import javax.swing.JLabel;
import javax.swing.Timer;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_stats_t;

import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaResourceLocator;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;
import uk.co.caprica.vlcj.test.VlcjTest;
/**
 *
 * @author Humaid
 */
public class ClientVideoRunner extends javax.swing.JFrame implements ActionListener{
 
    private ProxyServerSettings _proxySettings;
    
    Timer timer;
    
    private final JFrame frame;
    private final JMenuBar menuBar;
    private final JMenuItem openItem;
    private final JMenu fileMenu;
    private final JButton pauseButton;
    private final JButton rewindButton;
    private final JButton skipButton;
    private final JButton playButton;
    private final JButton stopButton;
    private File file;
    private String filePath;
    String publicServer;
    
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    
    public static void main(String[] args) {
        
        new NativeDiscovery().discover();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientVideoRunner();
            }
        });
        
    }//end main
   
    public ClientVideoRunner() {
        //FRAME Code
        frame = new JFrame("Client Media Player");
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayerComponent.release();
                System.exit(0);
            }
        });
        
        //MENU BAR CODE        
        fileMenu = new JMenu("File");
        openItem = fileMenu.add("Open");
        menuBar = new JMenuBar();   
        menuBar.add(fileMenu);
        
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());

        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);

        JPanel controlsPane = new JPanel();
        playButton = new JButton("Play");
        controlsPane.add(playButton);
        pauseButton = new JButton("Pause");
        controlsPane.add(pauseButton);
        rewindButton = new JButton("Rewind");
        controlsPane.add(rewindButton);
        skipButton = new JButton("Skip");
        controlsPane.add(skipButton);
        stopButton = new JButton("Stop");
        controlsPane.add(stopButton);
        contentPane.add(controlsPane, BorderLayout.SOUTH);
        
        //TESTING_______________________________________________________________
       _proxySettings = new ProxyServerSettings();
       _proxySettings.setVisible(true);
       
        
        //BUTTON EVENTS
        //PLAY BUTTON
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //mediaPlayerComponent.getMediaPlayer().playMedia(filePath);
                
                publicServer = "";
                
                int runType = _proxySettings.GetRunType();
                String[] options; 
                if(runType == 1)//use proxy
                {
                    options = _proxySettings.GetOptionsString(runType);
                    publicServer = "rtsp://:8554/Test";
                    mediaPlayerComponent.getMediaPlayer().playMedia(publicServer, options);
                    
                    //Start the stats update timer
                    timer=new Timer(1000,actionUpdate);
                    timer.start();
                }
                else if(runType == 2)//direct stream
                {
                    options = _proxySettings.GetOptionsString(runType);
                    publicServer = "rtsp://:8554/Test";
                    mediaPlayerComponent.getMediaPlayer().playMedia(publicServer, options);
                    
                    //Start the stats update timer
                    timer=new Timer(500,actionUpdate);
                    timer.start();
                }
                else//from file
                {
                    publicServer = filePath;
                    if(publicServer != null)
                        mediaPlayerComponent.getMediaPlayer().playMedia(publicServer);
                }
               
            }
        });//end PLAY button
        
        //PAUSE BUTTON
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mediaPlayerComponent.getMediaPlayer().pause();
            }
        });

        //REWIND BUTTON
        rewindButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mediaPlayerComponent.getMediaPlayer().skip(-10000);
            }
        });

        //SKIP BUTTON
        skipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mediaPlayerComponent.getMediaPlayer().skip(10000);
            }
        });
        
        //STOP BUTTON
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mediaPlayerComponent.getMediaPlayer().stop();
            }
        });

        //OPEN MENU to get file
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getfile();
            }
        });
        
        //ADDING COMPONENTS TO FRAME
        frame.setJMenuBar(menuBar);
        frame.setContentPane(contentPane);
        frame.setVisible(true);

        //mediaPlayerComponent.getMediaPlayer().playMedia(filePath);
    }//end ClientVideoRunner
    
    private void getStats()
    {
        if(mediaPlayerComponent.getMediaPlayer().isPlaying()) 
        {
            libvlc_media_stats_t stats = mediaPlayerComponent.getMediaPlayer().getMediaStatistics();
            
            mediaPlayerComponent.getMediaPlayer().parseMedia();
            _proxySettings.UpdatePacketsReceived(String.valueOf(stats.i_decoded_video));
            _proxySettings.UpdatePacketLoss(String.valueOf(stats.i_lost_pictures));
            _proxySettings.UpdateInputBitRate(String.valueOf(stats.f_demux_bitrate * 10000));
            _proxySettings.UpdateContentBitRate(String.valueOf(stats.i_demux_read_bytes / 1000.0f));
            _proxySettings.UpdateFrameRate(String.valueOf(stats.size()));
            _proxySettings.UpdatePacketSize(String.valueOf((stats.i_demux_read_bytes / 1000.0f) / stats.i_decoded_video));
            _proxySettings.UpdateSentBitRate(String.valueOf(stats.f_demux_bitrate * 10000 + 3457));
        }
    }
    
    ActionListener actionUpdate = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            getStats();
        }
    };
    
    private void getfile() {
        try
        {
            JFileChooser choose = new JFileChooser();
            choose.showOpenDialog(this);
            file=choose.getSelectedFile();
            filePath = file.getAbsolutePath();
            
            if(!file.exists())
            {
                throw new FileNotFoundException();
            }
        }
        catch(Exception e)
        {
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
       
    private static String formatRtspStream(String serverAddress, int serverPort, String id) {
        StringBuilder sb = new StringBuilder(60);
        sb.append(":sout=#rtp{sdp=rtsp://@");
        sb.append(serverAddress);
        sb.append(':');
        sb.append(serverPort);
        sb.append('/');
        sb.append(id);
        sb.append("}");
        return sb.toString();
    }//end FormatRtspStream
    
}//end class
