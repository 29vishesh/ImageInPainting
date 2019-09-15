package imageselection;

import inpaint.ImageInpaint;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
//import javax.swing.JMenu;
//import javax.swing.JMenuBar;
//import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main extends JFrame
        implements Runnable {

    private javax.swing.JLabel jLabel2;
    public Entry entry;
    protected Image entryImage;
    JScrollPane pictureScrollPane;
    File outputFile;
    String fileExtension;
    ImageInpaint Inpainter;
    Thread inpaintThread = null;
    private Boolean fastInpaint = Boolean.valueOf(false);
    int m_h, m_w, m_type;
    String inputFileName = "";
    String inputFileType = "";
    
    
   public Main(File imageee) {
        this.Inpainter = new ImageInpaint(this);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        inputFileName = imageee.getName();
        inputFileType = imageee.getName().substring(imageee.getName().lastIndexOf(".")+1);
        
        //setDefaultCloseOperation(3);
        initComponents();
        getContentPane().setLayout(new FlowLayout());
        File resizeImage = new File("O"+imageee.getName());
        try {
            //Resize image to small resultion.
            resize(imageee.getAbsolutePath(), resizeImage.getName(), 400, 500);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            this.entryImage = ImageIO.read(resizeImage);
        } catch (IOException ex) {
            Logger.getLogger(Entry.class.getName()).log(Level.SEVERE, null, ex);
        }

        setTitle("Super-Resolution-based Inpainting");
        setResizable(false);
        //getContentPane().setLayout(null);
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(700, 800);

        jLabel2 = new JLabel();
        jLabel2.setBounds(10,10,20,40);
        jLabel2.setFont(new java.awt.Font("Cambria Math", 2, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(51, 102, 255));
        jLabel2.setText("          Super-Resolution Based Inpainting          ");
        jLabel2.setName("jLabel2");
        jLabel2.setEnabled(true);

        getContentPane().add(jLabel2);

        JButton startButton = new JButton("Start");
        add(startButton);

        JButton saveButton = new JButton("Save");
        add(saveButton);


        startButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
                startInpainting();
           }
        });

       saveButton.addActionListener(new ActionListener() {

           public void actionPerformed(ActionEvent e) {
               JFileChooser chooser = new JFileChooser();
               chooser.setCurrentDirectory(new java.io.File("."));
               chooser.setDialogTitle("choosertitle");
               chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
               chooser.setAcceptAllFileFilterUsed(false);

               if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                   //System.out.println("getCurrentDirectory(): " + chooser.getCurrentDirectory());
                   System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
                   BufferedImage finalImage = improveResoultion(entryImage);
                   File outputfile = new File(chooser.getSelectedFile() + File.separator + "output-" + inputFileName);
                    try {
                        ImageIO.write(finalImage, inputFileType, outputfile);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
               } else {
                   System.out.println("No Selection ");
               }
          }
       });

       //getContentPane().setBackground(new java.awt.Color(0, 111, 1));
        this.entry = new Entry(this.entryImage);
        this.entry.setPreferredSize(new Dimension(this.entryImage.getWidth(this), this.entryImage.getHeight(this)));
        getContentPane().add(this.entry);
        this.entry.initImage();

        this.pictureScrollPane = new JScrollPane();
        getContentPane().add(this.pictureScrollPane);
        int w = Math.min(this.entryImage.getWidth(this) + 3, getContentPane().getWidth());
        int h = Math.min(this.entryImage.getHeight(this) + 3, getContentPane().getHeight() - 50);
        if (h == this.entryImage.getHeight(this) + 3) {
            this.pictureScrollPane.setBounds((getContentPane().getWidth() - w) / 2, (getContentPane().getHeight() - h) / 2, w, h);
        } else {
            this.pictureScrollPane.setBounds((getContentPane().getWidth() - w) / 2, (getContentPane().getHeight() - h) / 2, w, h + 25);
        }

        this.pictureScrollPane.setAlignmentY(0.5F);
        this.pictureScrollPane.setVerticalScrollBarPolicy(20);
        this.pictureScrollPane.setHorizontalScrollBarPolicy(30);
        this.pictureScrollPane.setViewportView(this.entry);

        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                int w = Math.min(Main.this.entryImage.getWidth(Main.this.entry) + 3, Main.this.getContentPane().getWidth());
                int h = Math.min(Main.this.entryImage.getHeight(Main.this.entry) + 3, Main.this.getContentPane().getHeight() - 50);
                if (h == Main.this.entryImage.getHeight(Main.this.entry) + 3) {
                    Main.this.pictureScrollPane.setBounds((Main.this.getContentPane().getWidth() - w) / 2, (Main.this.getContentPane().getHeight() - h) / 2, w, h);
                } else {
                    Main.this.pictureScrollPane.setBounds((Main.this.getContentPane().getWidth() - w) / 2, (Main.this.getContentPane().getHeight() - h) / 2, w, h + 25);
                }

                Main.this.pictureScrollPane.setViewportView(Main.this.entry);
            }
        });
        setImage(resizeImage);
    }

    /*public static void main(String[] args) {
        //new Main(new File("C:\\Users\\Lenovo\\Desktop\\Super-Resolution-based Inpainting\\input image.jpg")).show();
        new Main(new File("C:\\Users\\Lenovo\\Desktop\\3.jpg")).show();
    }*/

    private BufferedImage improveResoultion(Image image){

        //BufferedImage bb = image;
        if(m_type == 0)
            m_type = BufferedImage.TYPE_INT_ARGB;

	BufferedImage resizedImage = new BufferedImage(m_w, m_h, m_type);
	Graphics2D g = resizedImage.createGraphics();

	g.setComposite(AlphaComposite.Src);

	g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
	g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
	g.drawImage(image, 0, 0, m_w, m_h, null);
	g.dispose();
        return resizedImage;
    }

    private void initComponents() {
        
        setDefaultCloseOperation(3);

//    jMenuItem9.setAccelerator(KeyStroke.getKeyStroke(70, 2));
//
//    jMenuItem9.setText("Fast Inpaint");
//    jMenu3.add(jMenuItem9);
//    jMenuItem9.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent evt) {
//        Main.this.jMenuItem9ActionPerformed(evt);
//      }
//    });

//        GroupLayout layout = new GroupLayout(getContentPane());
//        getContentPane().setLayout(layout);
//        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 400, 32767));
//        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGap(0, 279, 32767));
        setDefaultCloseOperation(3);
        pack();
    }

    public void resize(String inputImagePath, String outputImagePath, int scaledWidth, int scaledHeight)
            throws IOException {
        //reads input image
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        m_w = inputImage.getWidth();
        m_h = inputImage.getHeight();
        m_type = inputImage.getType();
        
        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

        //scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        //extracts extension of output file
        String formatName = outputImagePath.substring(outputImagePath.lastIndexOf(".") + 1);

        //writes to output file
        ImageIO.write(outputImage, formatName, new File(outputImagePath));
    }

    //Openfile
    private void setImage(File file) {
        if (!this.entry.isDisabled.booleanValue()) {
            //JFileChooser _fileChooser = new JFileChooser();
            //int retval = _fileChooser.showOpenDialog(this);

            //String[] okFileExtensions = {"jpg", "png", "gif", "bmp", "jpeg"};

            //if (retval == 0)
            {
                try {
//                     = _fileChooser.getSelectedFile();
//                    Boolean flag = Boolean.valueOf(false);
//                    for (String extension : okFileExtensions) {
//                        if (file.getName().toLowerCase().endsWith(extension)) {
//                            this.outputFile = file;
//                            this.fileExtension = extension;
//                            flag = Boolean.valueOf(true);
//                        }
//                    }
//                    if (!flag.booleanValue()) {
//                        JOptionPane.showMessageDialog(this, "Please choose a jpg, jpeg, png, bmp or gif file only.", "Error", 0);
//                        return;
//                    }

                    this.entry.SavedImages.clear();
                    this.entry.RedoImages.clear();
                    BufferedImage selectedImage = ImageIO.read(file);
                    Image tmg = createImage(selectedImage.getWidth(this), selectedImage.getHeight(this));
                    Graphics tg = tmg.getGraphics();
                    tg.drawImage(selectedImage, 0, 0, null);
                    this.entry.SavedImages.push(selectedImage);
                    this.entryImage = tmg;
                    this.entry.showImage(this.entryImage);
                    this.entry.setPreferredSize(new Dimension(this.entryImage.getWidth(this), this.entryImage.getHeight(this)));
                    int w = Math.min(this.entryImage.getWidth(this) + 3, getContentPane().getWidth());
                    int h = Math.min(this.entryImage.getHeight(this) + 3, getContentPane().getHeight());
                    this.pictureScrollPane.setBounds((getContentPane().getWidth() - w) / 2, (getContentPane().getHeight() - h) / 2, w, h);
                    this.pictureScrollPane.setViewportView(this.entry);
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    ex.printStackTrace();
                }
            }
        }
    }

    //REDO
    private void jMenuItem4ActionPerformed(ActionEvent evt) {
        Boolean flag = Boolean.valueOf(false);

        if ((!this.entry.isDisabled.booleanValue()) && (this.entry.SavedImages.size() > 1)) {
            if (this.entry.getPressed().booleanValue()) {
                this.entry.entryReset();
                flag = Boolean.valueOf(true);
            }
            this.entry.RedoImages.push(this.entry.SavedImages.pop());
            Image tmg = createImage(((Image) this.entry.SavedImages.peek()).getWidth(this), ((Image) this.entry.SavedImages.peek()).getHeight(this));
            Graphics tg = tmg.getGraphics();
            tg.drawImage((Image) this.entry.SavedImages.peek(), 0, 0, null);
            this.entry.showImage(tmg);

            this.entry.setPreferredSize(new Dimension(this.entryImage.getWidth(this), this.entryImage.getHeight(this)));
            int w = Math.min(this.entryImage.getWidth(this) + 3, getContentPane().getWidth());
            int h = Math.min(this.entryImage.getHeight(this) + 3, getContentPane().getHeight());
            this.pictureScrollPane.setBounds((getContentPane().getWidth() - w) / 2, (getContentPane().getHeight() - h) / 2, w, h);
            this.pictureScrollPane.setViewportView(this.entry);

            if (flag.booleanValue()) {
                jMenuItem5ActionPerformed(evt);
            }
        }
    }

    private void jMenuItem5ActionPerformed(ActionEvent evt) {
        if ((!this.entry.isDisabled.booleanValue()) && (this.entry.RedoImages.size() > 0)) {
            Image tmg = createImage(((Image) this.entry.RedoImages.peek()).getWidth(this), ((Image) this.entry.RedoImages.peek()).getHeight(this));
            Graphics tg = tmg.getGraphics();
            tg.drawImage((Image) this.entry.RedoImages.peek(), 0, 0, null);
            this.entry.showImage(tmg);
            this.entry.SavedImages.push(this.entry.RedoImages.pop());

            this.entry.setPreferredSize(new Dimension(this.entryImage.getWidth(this), this.entryImage.getHeight(this)));
            int w = Math.min(this.entryImage.getWidth(this) + 3, getContentPane().getWidth());
            int h = Math.min(this.entryImage.getHeight(this) + 3, getContentPane().getHeight());
            this.pictureScrollPane.setBounds((getContentPane().getWidth() - w) / 2, (getContentPane().getHeight() - h) / 2, w, h);
            this.pictureScrollPane.setViewportView(this.entry);
        }
    }


//    private void jMenuItem2ActionPerformed(ActionEvent evt) {
//        if (this.outputFile == null) {
//            System.err.println("Error!! No file to save");
//            return;
//        }
//        try {
//            BufferedImage bi = (BufferedImage) this.entry.getImage();
//            ImageIO.write(bi, this.fileExtension, this.outputFile);
//        } catch (IOException e) {
//            System.err.println("Error!! File not saved");
//        }
//    }

    //To Save output file...
    private void jMenuItem7ActionPerformed(ActionEvent evt) {
        JFileChooser _fileChooser = new JFileChooser();
        int retval = _fileChooser.showSaveDialog(this);

        String[] okFileExtensions = {"jpg", "png", "gif", "bmp", "jpeg"};

        if (retval == 0) {
            File file = _fileChooser.getSelectedFile();
            Boolean flag = Boolean.valueOf(false);
            for (String extension : okFileExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    if (this.outputFile == null) {
                        System.err.println("Error!! No file to save");
                        return;
                    }
                    try {
                        this.outputFile = file;
                        this.fileExtension = extension;
                        BufferedImage bi = (BufferedImage) this.entry.getImage();
                        ImageIO.write(bi, this.fileExtension, this.outputFile);
                        System.out.println("Saved");
                    } catch (IOException e) {
                        System.err.println("Error!! File not saved");
                    }

                    flag = Boolean.valueOf(true);
                }
            }
            if (!flag.booleanValue()) {
                JOptionPane.showMessageDialog(this, "Please choose a jpg, jpeg, png, bmp or gif file only.", "Error", 0);
                return;
            }
        }
    }

//    private void jMenuItem8ActionPerformed(ActionEvent evt) {
//        this.Inpainter.halt = Boolean.valueOf(false);
//        this.Inpainter.completed = Boolean.valueOf(false);
//        if (this.inpaintThread == null) {
//            this.entry.setDisabled();
//            this.inpaintThread = new Thread(this);
//            this.inpaintThread.start();
//        }
//    }

    //Do Inpainting
    private void startInpainting() {
        this.fastInpaint = Boolean.valueOf(true);
        this.Inpainter.halt = Boolean.valueOf(false);
        this.Inpainter.completed = Boolean.valueOf(false);
        if (this.inpaintThread == null) {
            this.entry.setDisabled();
            this.inpaintThread = new Thread(this);
            this.inpaintThread.start();
        }
    }

    private void PauseActionPerformed(ActionEvent evt) {
        if (this.inpaintThread != null) {
            this.Inpainter.halt = Boolean.valueOf(true);
        }
    }

    private void jMenuItem3ActionPerformed(ActionEvent evt) {
        System.exit(0);
    }

    public void run() {
        this.Inpainter.init((BufferedImage) this.entry.getImage(), (BufferedImage) this.entry.getImage(), this.fastInpaint);
    }

    public void updateStats(BufferedImage toShow) {
        UpdateStats stats = new UpdateStats();
        stats.toShow = toShow;
        try {
            SwingUtilities.invokeAndWait(stats);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e, "Training", 0);
        }

        if (this.Inpainter.completed.booleanValue()) {
            JOptionPane.showMessageDialog(this, "                      Inpainting is completed.", "Inpainting", -1);
        }

        if ((this.Inpainter.completed.booleanValue()) || (this.Inpainter.halt.booleanValue())) {
            System.out.println("Inpainting completed or halted");
            this.inpaintThread = null;
            Image tmg = createImage(toShow.getWidth(this), toShow.getHeight(this));
            Graphics tg = tmg.getGraphics();
            tg.drawImage(toShow, 0, 0, null);
            this.entry.SavedImages.push(tmg);
            this.entry.setEnabled();
            this.entry.RedoImages.clear();
            this.fastInpaint = Boolean.valueOf(false);
        }
    }

    public class UpdateStats implements Runnable {

        BufferedImage toShow;

        public UpdateStats() {
        }

        public void run() {
            Main.this.entry.showImage(this.toShow);
        }
    }

    class SymAction
            implements ActionListener {

        SymAction() {
        }

        public void actionPerformed(ActionEvent event) {
            Object object = event.getSource();
        }
    }
}
