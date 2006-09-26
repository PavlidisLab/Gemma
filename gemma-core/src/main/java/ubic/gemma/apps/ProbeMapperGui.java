/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.apps;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.apps.ProbeMapperCli;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperGui extends JFrame {
    private static Log log = LogFactory.getLog( ProbeMapperGui.class.getName() );
    private javax.swing.JPanel jContentPane = null;
    private JPanel topButtonPanel = null;
    private JPanel fileNamesPanel = null;
    private JPanel bottomPanel = null;
    private JButton okButton = null;
    private JButton cancelButton = null;
    private JPanel inputFileNamePanel = null;
    private JPanel outputFileNamePanel = null;
    private JTextField inputFileNameTextField = null;
    private JButton inputFileBrowseButton = null;
    private JTextField outputFileNameTextField = null;
    private JButton outputFileBrowseButton = null;
    private JPanel locationMethodPanel = null;
    private JLabel locationMethodLabel = null;
    JComboBox locationMethodComboBox = null;

    ThreePrimeDistanceMethod method = ThreePrimeDistanceMethod.RIGHT;
    protected File inputFile;
    protected File outputFile;

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getTopButtonPanel() {
        if ( topButtonPanel == null ) {
            topButtonPanel = new JPanel();
            topButtonPanel.setLayout( null );
            topButtonPanel.add( getLocationMethodPanel(), null );
        }
        return topButtonPanel;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getFileNamesPanel() {
        if ( fileNamesPanel == null ) {
            fileNamesPanel = new JPanel();
            fileNamesPanel.setLayout( null );
            fileNamesPanel.add( getInputFileNamePanel(), null );
            fileNamesPanel.add( getOutputFileNamePanel(), null );
        }
        return fileNamesPanel;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getBottomPanel() {
        if ( bottomPanel == null ) {
            bottomPanel = new JPanel();
            bottomPanel.setLayout( null );
            bottomPanel.add( getOkButton(), null );
            bottomPanel.add( getCancelButton(), null );
        }
        return bottomPanel;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if ( okButton == null ) {
            okButton = new JButton();
            okButton.setText( "OK" );
            okButton.setBounds( 200, 13, 51, 26 );
            okButton.addActionListener( new ActionListener() {

                @SuppressWarnings( { "unused", "synthetic-access" })
                public void actionPerformed( ActionEvent e ) {
                    inputFile = new File( inputFileNameTextField.getText() );
                    outputFile = new File( outputFileNameTextField.getText() );
                    if ( inputFile != null || outputFile != null ) {
                        run();
                    } else {
                        log.error( "Must provide file names" );
                    }

                }
            } );
        }
        return okButton;
    }

    /**
     * 
     */
    protected void run() {
        ProbeMapperCli ptpl = new ProbeMapperCli();
        try {
            String bestOutputFileName = outputFile.getAbsolutePath().replaceFirst( "\\.", ".best." );
            log.info( "Saving best to " + bestOutputFileName );
            Map<String, Collection<BlatAssociation>> results = ptpl.runOnBlatResults( new FileInputStream( inputFile ),
                    new BufferedWriter( new FileWriter( outputFile ) ) );
            File o = new File( bestOutputFileName );
            ptpl.printBestResults( results, new BufferedWriter( new FileWriter( o ) ) );
        } catch ( FileNotFoundException e ) {
            log.error( e, e );
        } catch ( IOException e ) {
            log.error( e, e );
        } catch ( SQLException e ) {
            log.error( e, e );
        }

    }

    /**
     * This method initializes jButton1
     * 
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        if ( cancelButton == null ) {
            cancelButton = new JButton();
            cancelButton.setText( "Cancel" );
            cancelButton.setBounds( 256, 13, 73, 26 );
            cancelButton.addActionListener( new ActionListener() {
                @SuppressWarnings("unused")
                public void actionPerformed( ActionEvent e ) {
                    exit();
                }
            } );
        }
        return cancelButton;
    }

    /**
     * 
     */
    protected void exit() {
        System.exit( 0 );
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getInputFileNamePanel() {
        if ( inputFileNamePanel == null ) {
            inputFileNamePanel = new JPanel();
            inputFileNamePanel.setLayout( null );
            inputFileNamePanel.setBounds( 16, 22, 544, 41 );
            inputFileNamePanel.add( getInputFileNameTextField(), null );
            inputFileNamePanel.add( getInputFileBrowseButton(), null );
        }
        return inputFileNamePanel;
    }

    /**
     * This method initializes jPanel2
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getOutputFileNamePanel() {
        if ( outputFileNamePanel == null ) {
            outputFileNamePanel = new JPanel();
            outputFileNamePanel.setLayout( null );
            outputFileNamePanel.setBounds( 15, 71, 548, 35 );
            outputFileNamePanel.add( getOutputFileNameTextField(), null );
            outputFileNamePanel.add( getOutputFileBrowseButton(), null );
        }
        return outputFileNamePanel;
    }

    /**
     * This method initializes getInputFileNameTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getInputFileNameTextField() {
        if ( inputFileNameTextField == null ) {
            inputFileNameTextField = new JTextField();
            inputFileNameTextField.setPreferredSize( new java.awt.Dimension( 200, 20 ) );
            inputFileNameTextField.setLocation( 5, 12 );
            inputFileNameTextField.setSize( 440, 20 );
            inputFileNameTextField.setText( "Input File" );
        }
        return inputFileNameTextField;
    }

    /**
     * This method initializes InputFileBrowseButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getInputFileBrowseButton() {
        if ( inputFileBrowseButton == null ) {
            inputFileBrowseButton = new JButton();
            inputFileBrowseButton.setText( "Browse..." );
            inputFileBrowseButton.setActionCommand( "inputFileBrowse" );
            inputFileBrowseButton.setBounds( 451, 8, 87, 26 );
            inputFileBrowseButton.addActionListener( new ActionListener() {

                @SuppressWarnings( { "unused", "synthetic-access" })
                public void actionPerformed( ActionEvent e ) {

                    JFileChooser fc = new JFileChooser();
                    fc.showOpenDialog( jContentPane.getParent() );
                    File selectedFile = fc.getSelectedFile();

                    if ( selectedFile != null ) {
                        getInputFileNameTextField().setText( selectedFile.getAbsolutePath() );
                        if ( !selectedFile.canRead() ) {
                            // error
                        } else {
                            inputFile = selectedFile;
                        }
                    }
                }
            } );
        }
        return inputFileBrowseButton;
    }

    /**
     * This method initializes OutputFileNameTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getOutputFileNameTextField() {
        if ( outputFileNameTextField == null ) {
            outputFileNameTextField = new JTextField();
            outputFileNameTextField.setPreferredSize( new java.awt.Dimension( 200, 20 ) );
            outputFileNameTextField.setText( "Output file" );
            outputFileNameTextField.setBounds( 6, 8, 440, 20 );
        }
        return outputFileNameTextField;
    }

    /**
     * @return javax.swing.JButton
     */
    private JButton getOutputFileBrowseButton() {
        if ( outputFileBrowseButton == null ) {
            outputFileBrowseButton = new JButton();
            outputFileBrowseButton.setText( "Browse..." );
            outputFileBrowseButton.setBounds( 454, 4, 87, 26 );

            outputFileBrowseButton.addActionListener( new ActionListener() {
                @SuppressWarnings( { "unused", "synthetic-access" })
                public void actionPerformed( ActionEvent e ) {
                    JFileChooser fc = new JFileChooser();
                    fc.showSaveDialog( jContentPane.getParent() );
                    File selectedFile = fc.getSelectedFile();
                    if ( selectedFile != null ) {
                        getOutputFileNameTextField().setText( selectedFile.getAbsolutePath() );
                        if ( selectedFile.canRead() ) {
                            outputFile = selectedFile;
                        } else {
                            // error
                        }
                    }
                }
            } );
        }
        return outputFileBrowseButton;
    }

    /**
     * This method initializes jPanel3
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getLocationMethodPanel() {
        if ( locationMethodPanel == null ) {
            locationMethodLabel = new JLabel();
            locationMethodPanel = new JPanel();
            locationMethodPanel.setLayout( null );
            locationMethodLabel.setText( "Location Method" );
            locationMethodLabel.setBounds( 81, 19, 123, 16 );
            locationMethodLabel.setFont( new java.awt.Font( "Dialog", java.awt.Font.PLAIN, 12 ) );
            locationMethodPanel.setBounds( 0, 0, 594, 46 );
            locationMethodPanel.add( locationMethodLabel, null );
            locationMethodPanel.add( getLocationMethodComboBox(), null );
        }
        return locationMethodPanel;
    }

    /**
     * This method initializes jComboBox
     * 
     * @return javax.swing.JComboBox
     */
    private JComboBox getLocationMethodComboBox() {
        if ( locationMethodComboBox == null ) {
            locationMethodComboBox = new JComboBox();
            locationMethodComboBox.setBounds( 209, 17, 234, 20 );
            locationMethodComboBox.addItem( "Center" );
            locationMethodComboBox.addItem( "3' end" );
            locationMethodComboBox.addActionListener( new java.awt.event.ActionListener() {

                @SuppressWarnings("unused")
                public void actionPerformed( java.awt.event.ActionEvent e ) {
                    if ( ( ( String ) locationMethodComboBox.getSelectedItem() )
                            .equals( ThreePrimeDistanceMethod.MIDDLE ) ) {
                        method = ThreePrimeDistanceMethod.MIDDLE;
                    } else if ( ( ( String ) locationMethodComboBox.getSelectedItem() )
                            .equals( ThreePrimeDistanceMethod.RIGHT ) ) {
                        method = ThreePrimeDistanceMethod.RIGHT;
                    }
                }
            } );

        }
        return locationMethodComboBox;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( InstantiationException e ) {
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        } catch ( UnsupportedLookAndFeelException e ) {
            e.printStackTrace();
        }
        ProbeMapperGui pgmg = new ProbeMapperGui();

        pgmg.pack();
        pgmg.setVisible( true );

    }

    /**
     * This is the default constructor
     */
    public ProbeMapperGui() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setDefaultCloseOperation( javax.swing.JFrame.EXIT_ON_CLOSE );
        this.setSize( 602, 472 );
        this.setContentPane( getJContentPane() );
        this.setTitle( "ProbeMapper" );
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private javax.swing.JPanel getJContentPane() {
        if ( jContentPane == null ) {
            GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            jContentPane = new javax.swing.JPanel();
            jContentPane.setLayout( new GridBagLayout() );
            gridBagConstraints3.insets = new java.awt.Insets( 0, 0, 18, 0 );
            gridBagConstraints3.gridx = 0;
            gridBagConstraints3.gridy = 0;
            gridBagConstraints3.ipadx = 593;
            gridBagConstraints3.ipady = 219;
            gridBagConstraints4.insets = new java.awt.Insets( 19, 0, 4, 0 );
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.gridy = 1;
            gridBagConstraints4.ipadx = 593;
            gridBagConstraints4.ipady = 124;
            gridBagConstraints5.insets = new java.awt.Insets( 5, 37, 1, 31 );
            gridBagConstraints5.gridx = 0;
            gridBagConstraints5.gridy = 2;
            gridBagConstraints5.ipadx = 525;
            gridBagConstraints5.ipady = 50;
            jContentPane.add( getTopButtonPanel(), gridBagConstraints3 );
            jContentPane.add( getFileNamesPanel(), gridBagConstraints4 );
            jContentPane.add( getBottomPanel(), gridBagConstraints5 );
        }
        return jContentPane;
    }

} // @jve:decl-index=0:visual-constraint="10,10"
