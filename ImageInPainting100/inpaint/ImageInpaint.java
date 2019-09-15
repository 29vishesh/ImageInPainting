package inpaint;

import imageselection.Main;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.WritableRaster;
import java.io.PrintStream;
import java.util.Vector;
import javax.swing.JOptionPane;

public class ImageInpaint
{
  BufferedImage origImg;
  Image fillImg;
  BufferedImage img;
  WritableRaster raster;
  int iw;
  int ih;
  int[] pixels;
  int[][] pixelmap;
  int[][] fillPixelmap;
  int[][] sourceRegion;
  int[][] initialSourceRegion;
  double[][] fillRegion;
  double[][] gradientX;
  double[][] gradientY;
  double[][] confidence;
  double[][] data;
  GradientCalculator gc;
  double omega = 0.7D;
  double Alpha = 0.2D;
  double Beta = 0.8D;
  int maxX;
  int maxY;
  int minX;
  int minY;
  int i;
  int continuousCol = 0;
  int continuousRow = 0;
  protected Main owner;
  public Boolean halt = Boolean.valueOf(false);
  public Boolean completed = Boolean.valueOf(false);
  final int diamX = 50;
  final int diamY = 30;
  private int pixelPosX;
  private int pixelPosY;
  int w = 3;
  double[][] con = { { 1.0D, 1.0D, 1.0D }, { 1.0D, -8.0D, 1.0D }, { 1.0D, 1.0D, 1.0D } };

  public ImageInpaint(Main owner)
  {
    this.owner = owner;
  }

  public void init(BufferedImage a_origImg, BufferedImage a_fillImg, Boolean quickInpaint)
  {
    this.halt = Boolean.valueOf(false);
    try
    {
      this.origImg = a_origImg;
      this.img = a_origImg;
      this.fillImg = a_fillImg;
      this.raster = this.origImg.getRaster();

      this.iw = this.img.getWidth(null);
      this.ih = this.img.getHeight(null);

      this.pixels = new int[this.iw * this.ih];
      PixelGrabber pg = new PixelGrabber(this.img, 0, 0, this.iw, this.ih, this.pixels, 0, this.iw);
      pg.grabPixels();
      this.pixelmap = new int[this.ih][this.iw];

      for (i = 0; i < this.ih; i++) {
        for (int j = 0; j < this.iw; j++) {
          this.pixelmap[i][j] = this.pixels[(i * this.iw + j)];
        }

      }

      PixelGrabber fillPg = new PixelGrabber(this.fillImg, 0, 0, this.iw, this.ih, this.pixels, 0, this.iw);
      fillPg.grabPixels();
      this.fillPixelmap = new int[this.ih][this.iw];

      for (i = 0; i < this.ih; i++) {
        for (int j = 0; j < this.iw; j++)
          this.fillPixelmap[i][j] = this.pixels[(i * this.iw + j)];
      }
    }
    catch (InterruptedException e1)
    {
      JOptionPane.showMessageDialog(this.owner, "Error " + e1, "Error!!", 0);

      System.out.println("Error " + e1);
    }

    this.gc.calculateGradientFromImage(this.pixelmap, this.ih, this.iw);
    this.gradientX = this.gc.gradientX;
    this.gradientY = this.gc.gradientY;

    initialize_confidence_term();
    initialize_data_term();

    this.fillRegion = new double[this.ih][this.iw];
    this.sourceRegion = new int[this.ih][this.iw];
    this.initialSourceRegion = new int[this.ih][this.iw];

    this.minX = this.iw;
    this.minY = this.ih;
    this.maxX = (this.maxY = 0);

    this.continuousRow = (this.continuousCol = 0);
    for (int i = 0; i < this.ih; i++) {
      int countrow = 0;
      for (int j = 0; j < this.iw; j++) {
        int pixel = this.fillPixelmap[i][j];
        int r = 0xFF & pixel >> 16;
        int g = 0xFF & pixel >> 8;
        int b = 0xFF & pixel;

        if ((r == 0) && (g == 255) && (b == 0)) {
          countrow++;
          this.fillRegion[i][j] = 1.0D;
          this.sourceRegion[i][j] = 0;
          this.initialSourceRegion[i][j] = 0;
          if (j < this.minX) {
            this.minX = j;
          }
          if (i < this.minY) {
            this.minY = i;
          }
          if (j > this.maxX) {
            this.maxX = j;
          }
          if (i > this.maxY)
            this.maxY = i;
        }
        else {
          if (countrow > this.continuousRow) {
            this.continuousRow = countrow;
          }
          countrow = 0;
          this.fillRegion[i][j] = 0.0D;
          this.sourceRegion[i][j] = 1;
          this.initialSourceRegion[i][j] = 1;
        }
      }
    }

    for (i = 0; i < this.iw; i++) {
      int countcol = 0;
      for (int j = 0; j < this.ih; j++) {
        int pixel = this.fillPixelmap[j][i];
        int r = 0xFF & pixel >> 16;
        int g = 0xFF & pixel >> 8;
        int b = 0xFF & pixel;

        if ((r == 0) && (g == 255) && (b == 0)) {
          countcol++;
        } else {
          if (countcol > this.continuousCol) {
            this.continuousCol = countcol;
          }
          countcol = 0;
        }

      }

    }

    Boolean flag = Boolean.valueOf(true);

    double[][] temp = new double[this.ih][this.iw];
    double[][] sourceGradX = new double[this.ih][this.iw];
    double[][] sourceGradY = new double[this.ih][this.iw];

    Vector dR = new Vector();
    Vector Nx = new Vector();
    Vector Ny = new Vector();

    while (flag.booleanValue())
    {
      temp = conv2(this.fillRegion, this.con);

      this.gc.calculateGradient(this.sourceRegion, this.ih, this.iw);
      sourceGradX = this.gc.gradientX;
      sourceGradY = this.gc.gradientY;

      dR.clear();
      Nx.clear();
      Ny.clear();

      for (i = 0; i < temp[0].length; i++) {
        for (int j = 0; j < temp.length; j++) {
          if (temp[j][i] > 0.0D) {
            dR.add(Integer.valueOf(i * temp.length + j));
            Nx.add(Double.valueOf(sourceGradX[j][i]));
            Ny.add(Double.valueOf(sourceGradY[j][i]));
          }
        }
      }

      double[][] N = normr(Nx, Ny);

      Vector q = new Vector();
      double count = 0.0D;

      for (i = 0; i < dR.size(); i++) {
        int[][] Hp = getpatch(this.pixelmap, ((Integer)dR.get(i)).intValue());
        for (int j = 0; j < Hp.length; j++) {
          for (int k = 0; k < Hp[0].length; k++) {
            int col = Hp[j][k] / this.ih;
            int row = Hp[j][k] % this.ih;
            if (this.fillRegion[row][col] == 0.0D) {
              count += this.confidence[row][col];
              q.add(Integer.valueOf(Hp[j][k]));
            }
          }
        }

        int col = ((Integer)dR.get(i)).intValue() / this.ih;
        int row = ((Integer)dR.get(i)).intValue() % this.ih;
        this.confidence[row][col] = (count / (Hp.length * Hp[0].length));
        count = 0.0D;
      }

      double maxPriority = 0.0D;
      int maxPriorityIndex = -1;

      for (i = 0; i < dR.size(); i++) {
        int col = ((Integer)dR.get(i)).intValue() / this.ih;
        int row = ((Integer)dR.get(i)).intValue() % this.ih;

        this.data[row][col] = (Math.abs(this.gradientX[row][col] * N[i][0] + this.gradientY[row][col] * N[i][1]) + 0.001D);

        double Rcp = (1.0D - this.omega) * this.confidence[row][col] + this.omega;

        double tempPriority = this.Alpha * Rcp + this.Beta * this.data[row][col];

        if (tempPriority >= maxPriority) {
          maxPriority = tempPriority;
          maxPriorityIndex = i;
        }
      }

      if (maxPriorityIndex == -1)
      {
        break;
      }

      int[][] Hp = getpatch(this.pixelmap, ((Integer)dR.get(maxPriorityIndex)).intValue());
      double[][] toFill = new double[Hp.length][Hp[0].length];
      double[][] toFillTrans = new double[Hp[0].length][Hp.length];

      for (i = 0; i < Hp.length; i++) {
        for (int j = 0; j < Hp[0].length; j++) {
          int col = Hp[i][j] / this.ih;
          int row = Hp[i][j] % this.ih;
          toFill[i][j] = this.fillRegion[row][col];
          toFillTrans[j][i] = this.fillRegion[row][col];
        }
      }

      this.pixelPosX = (((Integer)dR.get(maxPriorityIndex)).intValue() / this.ih);
      this.pixelPosY = (((Integer)dR.get(maxPriorityIndex)).intValue() % this.ih);

      int[] best = bestExemplar(Hp, toFillTrans, this.initialSourceRegion, quickInpaint);

      int nRows = best[3] - best[2] + 1;
      int nCols = best[1] - best[0] + 1;

      int[][] X = new int[nRows][nCols];
      int[][] Y = new int[nRows][nCols];
      int[][] Hq = new int[nRows][nCols];
      for (i = 0; i < nRows; i++) {
        for (int j = 0; j < nCols; j++) {
          X[i][j] = (best[0] + j);
          Y[i][j] = (best[2] + i);
          X[i][j] += Y[i][j] * this.ih;
        }
      }

      int p = ((Integer)dR.get(maxPriorityIndex)).intValue();

      for (i = 0; i < toFill.length; i++) {
        for (int j = 0; j < toFill[0].length; j++) {
          if (toFill[i][j] != 0.0D) {
            toFill[i][j] = 1.0D;
            int col = Hp[i][j] / this.ih;
            int row = Hp[i][j] % this.ih;

            int col1 = Hq[i][j] / this.ih;
            int row1 = Hq[i][j] % this.ih;

            this.fillRegion[row][col] = 0.0D;
            this.sourceRegion[row][col] = 1;

            this.confidence[row][col] = this.confidence[(p % this.ih)][(p / this.ih)];
            this.gradientX[row][col] = this.gradientX[row1][col1];
            this.gradientY[row][col] = this.gradientY[row1][col1];
            this.pixelmap[row][col] = this.pixelmap[row1][col1];

            int[] color = new int[3];
            color[0] = (0xFF & this.pixelmap[row1][col1] >> 16);
            color[1] = (0xFF & this.pixelmap[row1][col1] >> 8);
            color[2] = (0xFF & this.pixelmap[row1][col1]);
            this.raster.setPixel(col, row, color);
          }
        }
      }

      if (this.halt.booleanValue()) break;
      this.owner.updateStats(this.origImg);
      Thread.yield();

      flag = Boolean.valueOf(false);
      for (i = 0; i < this.fillRegion.length; i++) {
        for (int j = 0; j < this.fillRegion[0].length; j++) {
          if (this.fillRegion[i][j] == 1.0D) {
            flag = Boolean.valueOf(true);
            break;
          }
        }
        if (flag.booleanValue())
          break;
      }
      if (this.halt.booleanValue())
      {
        break;
      }
    }

    this.gc = null;
    if (!this.halt.booleanValue())
      this.completed = Boolean.valueOf(true);
    else {
      this.completed = Boolean.valueOf(false);
    }
    this.owner.updateStats(this.origImg);
    Thread.yield();
  }

  void initialize_confidence_term()
  {
    this.confidence = new double[this.ih][this.iw];
    for (int i = 0; i < this.ih; i++)
      for (int j = 0; j < this.iw; j++) {
        int p = this.pixelmap[i][j];
        int r = 0xFF & p >> 16;
        int g = 0xFF & p >> 8;
        int b = 0xFF & p;
        if ((r == 0) && (g == 255) && (b == 0))
          this.confidence[i][j] = 0.0D;
        else
          this.confidence[i][j] = 1.0D;
      }
  }

  void initialize_data_term()
  {
    this.data = new double[this.ih][this.iw];
    for (int i = 0; i < this.ih; i++)
      for (int j = 0; j < this.iw; j++)
        this.data[i][j] = -0.1D;
  }

  double[][] conv2(double[][] a, double[][] b)
  {
    int ra = a.length;
    int ca = a[0].length;
    int rb = b.length;
    int cb = b[0].length;

    double[][] c = new double[ra + rb - 1][ca + cb - 1];
    for (int i = 0; i < rb; i++) {
      for (int j = 0; j < cb; j++) {
        int r1 = i;
        int r2 = r1 + ra - 1;
        int c1 = j;
        int c2 = c1 + ca - 1;
        for (int k = r1; k < r2; k++) {
          for (int l = c1; l < c2; l++) {
            c[k][l] += b[i][j] * a[(k - r1 + 1)][(l - c1 + 1)];
          }
        }
      }
    }

    double[][] out = new double[ra][ca];

    int r1 = rb / 2;
    int r2 = r1 + ra - 1;
    int c1 = cb / 2;
    int c2 = c1 + ca - 1;
    for (int i = r1; i < r2; i++) {
      for (int j = c1; j < c2; j++) {
        out[(i - r1 + 1)][(j - c1 + 1)] = c[i][j];
      }
    }
    return out;
  }

  double[][] normr(Vector X, Vector Y)
  {
    double[][] normalized = new double[X.size()][2];
    for (int i = 0; i < X.size(); i++) {
      double temp1 = ((Double)X.get(i)).doubleValue();
      double temp2 = ((Double)Y.get(i)).doubleValue();

      temp1 *= temp1;
      temp2 *= temp2;
      double temp3;

      if (temp1 + temp2 == 0.0D) {
        temp3 = 0.0D;
      } else {
        temp3 = 1.0D / (temp1 + temp2);
        temp3 = Math.sqrt(temp3);
      }

      normalized[i][0] = (temp3 * ((Double)X.get(i)).doubleValue());
      normalized[i][1] = (temp3 * ((Double)Y.get(i)).doubleValue());
    }

    return normalized;
  }

  int[][] getpatch(int[][] pixelmap, int p)
  {
    p -= 1;
    int y = p / this.ih;
    p %= this.ih;
    int x = p + 1;

    int temp1 = Math.max(x - this.w, 0);
    int temp2 = Math.min(x + this.w, this.ih - 1);
    int temp3 = Math.max(y - this.w, 0);
    int temp4 = Math.min(y + this.w, this.iw - 1);

    int[][] N = new int[temp4 - temp3 + 1][temp2 - temp1 + 1];

    for (int i = 0; i < temp4 - temp3 + 1; i++) {
      for (int j = 0; j < temp2 - temp1 + 1; j++)
      {
        N[i][j] = (temp1 + j + (temp3 + i) * this.ih);
      }
    }
    return N;
  }

  int[] bestExemplar(int[][] Hp, double[][] toFill, int[][] sourceRegion, Boolean quickInpaint)
  {
    int[][] Ip = new int[toFill.length][toFill[0].length];
    for (int i = 0; i < toFill[0].length; i++) {
      for (int j = 0; j < toFill.length; j++) {
        int col = Hp[i][j] / this.ih;
        int row = Hp[i][j] % this.ih;
        Ip[j][i] = this.pixelmap[row][col];
      }
    }

    int[][] rIp = new int[Ip.length][Ip[0].length];
    int[][] gIp = new int[Ip.length][Ip[0].length];
    int[][] bIp = new int[Ip.length][Ip[0].length];

    for (int i = 0; i < Ip.length; i++) {
      for (int j = 0; j < Ip[0].length; j++)
      {
        rIp[i][j] = (0xFF & Ip[i][j] >> 16);
        gIp[i][j] = (0xFF & Ip[i][j] >> 8);
        bIp[i][j] = (0xFF & Ip[i][j]);
      }

    }

    int m = Ip.length;
    int n = Ip[0].length;
    int nn;
    int mm;
    int startX;
    int startY;
    int endX;
    if (quickInpaint.booleanValue())
    {
       startX = Math.max(0, this.pixelPosX - n / 2 - this.continuousRow - 25);
      startY = Math.max(0, this.pixelPosY - m / 2 - this.continuousCol - 15);
      endX = Math.min(this.pixelmap[0].length - 1, this.pixelPosX + n / 2 + this.continuousRow + 25);
      int endY = Math.min(this.pixelmap.length - 1, this.pixelPosY + m / 2 + this.continuousCol + 15);
      mm = endY - startY + 1;
      nn = endX - startX + 1;
    } else {
      mm = this.pixelmap.length;
      nn = this.pixelmap[0].length;
      startX = 0;
      startY = 0;
      int endY;
      endX = endY = 0;
    }

    double patchErr = 0.0D; double err = 0.0D; double bestErr = 1.0E+020D; double bestPatchErr1 = 1.0E+020D;
    int[] best = { 0, 0, 0, 0 };
    Boolean skipPatchFlag = Boolean.valueOf(false);

    int N = startX + nn - n + 1; int M = startY + mm - m + 1;
    for (int j = startX; j < N; j++) {
      int J = j + n - 1;
      for (int i = startY; i < M; i++) {
        int I = i + m - 1;

        skipPatchFlag = Boolean.valueOf(false);

        double meanR = 0.0D;
        double meanG = 0.0D;
        double meanB = 0.0D;

        int jj = j; for (int jj2 = 0; jj <= J; jj2++) {
          int ii = i; for (int ii2 = 0; ii <= I; ii2++)
          {
            if (sourceRegion[ii][jj] != 1) {
              skipPatchFlag = Boolean.valueOf(true);
              break;
            }
            if (toFill[ii2][jj2] == 0.0D) {
              int rImage = 0xFF & this.pixelmap[ii][jj] >> 16;
              int gImage = 0xFF & this.pixelmap[ii][jj] >> 8;
              int bImage = 0xFF & this.pixelmap[ii][jj];

              err = rImage - rIp[ii2][jj2]; patchErr += err * err;
              err = gImage - gIp[ii2][jj2]; patchErr += err * err;
              err = bImage - bIp[ii2][jj2]; patchErr += err * err;

              meanR += rImage;
              meanG += gImage;
              meanB += bImage;
            }
            ii++;
          }

          if (skipPatchFlag.booleanValue())
            break;
          jj++;
        }

        if ((!skipPatchFlag.booleanValue()) && (patchErr < bestErr)) {
          bestErr = patchErr;
          best[0] = i; best[1] = I;
          best[2] = j; best[3] = J;

          double patchErr1 = 0.0D;
          jj = j; 
          for (int jj2 = 0; jj <= J; jj2++) {
            int ii = i; for (int ii2 = 0; ii <= I; ii2++) {
              if (toFill[ii2][jj2] == 1.0D) {
                int rImage = 0xFF & this.pixelmap[ii][jj] >> 16;
                int gImage = 0xFF & this.pixelmap[ii][jj] >> 8;
                int bImage = 0xFF & this.pixelmap[ii][jj];

                err = rImage - meanR; patchErr1 += err * err;
                err = gImage - meanG; patchErr1 += err * err;
                err = bImage - meanB; patchErr1 += err * err;
              }
              ii++;
            }
            jj++;
          }

          bestPatchErr1 = patchErr1;
        } else if ((!skipPatchFlag.booleanValue()) && (patchErr == bestErr))
        {
          double patchErr1 = 0.0D;
          jj = j; 
          for (int jj2 = 0; jj <= J; jj2++) {
            int ii = i; for (int ii2 = 0; ii <= I; ii2++) {
              if (toFill[ii2][jj2] == 1.0D) {
                int rImage = 0xFF & this.pixelmap[ii][jj] >> 16;
                int gImage = 0xFF & this.pixelmap[ii][jj] >> 8;
                int bImage = 0xFF & this.pixelmap[ii][jj];

                err = rImage - meanR; patchErr1 += err * err;
                err = gImage - meanG; patchErr1 += err * err;
                err = bImage - meanB; patchErr1 += err * err;
              }
              ii++;
            }
            jj++;
          }

          if (bestPatchErr1 > patchErr1) {
            best[0] = i; best[1] = I;
            best[2] = j; best[3] = J;
            bestPatchErr1 = patchErr1;
          }
        }

        patchErr = 0.0D;
      }
    }

    if ((best[0] == 0) && (best[1] == 0) && (best[2] == 0) && (best[3] == 0)) {
      return bestExemplar(Hp, toFill, sourceRegion, Boolean.valueOf(false));
    }
    return best;
  }
}
