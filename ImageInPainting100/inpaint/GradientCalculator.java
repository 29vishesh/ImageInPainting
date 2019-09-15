package inpaint;

import java.util.Vector;

public class GradientCalculator
{
  public double[][] gradientX;
  public double[][] gradientY;
  int r;
  int g;
  int b;

  public void calculateGradientFromImage(int[][] pixelmap, int ih, int iw)
  {
    this.gradientX = new double[ih][iw];
    this.gradientY = new double[ih][iw];

    int[] row = new int[ih];
    int[] column = new int[iw];

    for (int i = 0; i < ih; i++) {
      row[i] = (i + 1);
    }

    for (int i = 0; i < iw; i++) {
      column[i] = (i + 1);
    }

    if (ih > 1)
    {
      for (int j = 0; j < iw; j++) {
        extractRGB(pixelmap, 1, j);
        int r2 = this.r; int g2 = this.g; int b2 = this.b;

        extractRGB(pixelmap, 0, j);
        int r1 = this.r; int g1 = this.g; int b1 = this.b;

        double t1 = (r2 - r1) / (row[1] - row[0]);
        double t2 = (g2 - g1) / (row[1] - row[0]);
        double t3 = (b2 - b1) / (row[1] - row[0]);
        this.gradientX[0][j] = (-(t1 + t2 + t3) / 765.0D);

        extractRGB(pixelmap, ih - 1, j);
        int rn = this.r; int gn = this.g; int bn = this.b;

        extractRGB(pixelmap, ih - 2, j);
        int rn1 = this.r; int gn1 = this.g; int bn1 = this.b;

        t1 = (rn - rn1) / (row[(ih - 1)] - row[(ih - 2)]);
        t2 = (gn - gn1) / (row[(ih - 1)] - row[(ih - 2)]);
        t3 = (bn - bn1) / (row[(ih - 1)] - row[(ih - 2)]);
        this.gradientX[(ih - 1)][j] = (-(t1 + t2 + t3) / 765.0D);
      }
    }

    if (ih > 2)
    {
      for (int i = 1; i < ih - 1; i++) {
        for (int j = 0; j < iw; j++) {
          extractRGB(pixelmap, i + 1, j);
          int r2 = this.r; int g2 = this.g; int b2 = this.b;

          extractRGB(pixelmap, i - 1, j);
          int r1 = this.r; int g1 = this.g; int b1 = this.b;

          double t1 = (r2 - r1) / (row[(i + 1)] - row[(i - 1)]);
          double t2 = (g2 - g1) / (row[(i + 1)] - row[(i - 1)]);
          double t3 = (b2 - b1) / (row[(i + 1)] - row[(i - 1)]);
          this.gradientX[i][j] = (-(t1 + t2 + t3) / 765.0D);
        }
      }
    }

    if (iw > 1)
    {
      for (int j = 0; j < ih; j++) {
        extractRGB(pixelmap, j, 1);
        int r2 = this.r; int g2 = this.g; int b2 = this.b;

        extractRGB(pixelmap, j, 0);
        int r1 = this.r; int g1 = this.g; int b1 = this.b;

        double t1 = (r2 - r1) / (column[1] - column[0]);
        double t2 = (g2 - g1) / (column[1] - column[0]);
        double t3 = (b2 - b1) / (column[1] - column[0]);
        this.gradientY[j][0] = ((t1 + t2 + t3) / 765.0D);

        extractRGB(pixelmap, j, iw - 1);
        int rn = this.r; int gn = this.g; int bn = this.b;

        extractRGB(pixelmap, j, iw - 2);
        int rn1 = this.r; int gn1 = this.g; int bn1 = this.b;

        t1 = (rn - rn1) / (column[(iw - 1)] - column[(iw - 2)]);
        t2 = (gn - gn1) / (column[(iw - 1)] - column[(iw - 2)]);
        t3 = (bn - bn1) / (column[(iw - 1)] - column[(iw - 2)]);
        this.gradientY[j][(iw - 1)] = ((t1 + t2 + t3) / 765.0D);
      }
    }

    if (iw > 2)
    {
      for (int i = 0; i < ih; i++)
        for (int j = 1; j < iw - 1; j++)
        {
          extractRGB(pixelmap, i, j + 1);
          int r2 = this.r; int g2 = this.g; int b2 = this.b;

          extractRGB(pixelmap, i, j - 1);
          int r1 = this.r; int g1 = this.g; int b1 = this.b;

          double t1 = (r2 - r1) / (column[(j + 1)] - column[(j - 1)]);
          double t2 = (g2 - g1) / (column[(j + 1)] - column[(j - 1)]);
          double t3 = (b2 - b1) / (column[(j + 1)] - column[(j - 1)]);
          this.gradientY[i][j] = ((t1 + t2 + t3) / 765.0D);
        }
    }
  }

  public void calculateGradient(int[][] a, int ih, int iw)
  {
    this.gradientX = new double[ih][iw];
    this.gradientY = new double[ih][iw];
    Vector h = new Vector();
    int[] row = new int[ih];
    int[] column = new int[iw];

    for (int i = 0; i < ih; i++) {
      row[i] = (i + 1);
    }

    for (int i = 0; i < iw; i++) {
      column[i] = (i + 1);
    }

    if (ih > 1) {
      for (int j = 0; j < iw; j++) {
        this.gradientY[0][j] = ((a[1][j] - a[0][j]) / (row[1] - row[0]));
        this.gradientY[(ih - 1)][j] = ((a[(ih - 1)][j] - a[(ih - 2)][j]) / (row[(ih - 1)] - row[(ih - 2)]));
      }
    }

    if (ih > 2) {
      for (int i = 1; i < ih - 1; i++) {
        for (int j = 0; j < iw; j++) {
          this.gradientY[i][j] = ((a[(i + 1)][j] - a[(i - 1)][j]) / (row[(i + 1)] - row[(i - 1)]));
        }
      }
    }

    if (iw > 1) {
      for (int j = 0; j < ih; j++) {
        this.gradientX[j][0] = ((a[j][1] - a[j][0]) / (column[1] - column[0]));
        this.gradientX[j][(iw - 1)] = ((a[j][(iw - 1)] - a[j][(iw - 2)]) / (column[(iw - 1)] - column[(iw - 2)]));
      }
    }

    if (iw > 2)
      for (int i = 0; i < ih; i++)
        for (int j = 1; j < iw - 1; j++)
          this.gradientX[i][j] = ((a[i][(j + 1)] - a[i][(j - 1)]) / (column[(j + 1)] - column[(j - 1)]));
  }

  void extractRGB(int[][] img, int i, int j)
  {
    this.r = (0xFF & img[i][j] >> 16);
    this.g = (0xFF & img[i][j] >> 8);
    this.b = (0xFF & img[i][j]);
  }
}
