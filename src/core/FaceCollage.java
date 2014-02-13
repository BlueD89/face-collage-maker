package core;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class FaceCollage {
	// ######## CUSTOMIZATION PART ######## - start

	// The number of times, final collage image to be bigger than actual image,
	// so that the small images are clear when zoomed
	private static int enlargeSize = 4;

	// Number of pixels in each small block of the actual image. Smaller the
	// pixels, better the final collage. Higher the pixels, small images in
	// final collage look clear. Better to keep the same x-y ratio(4:3
	// generally) as your source images.
	private static int XpixelsPerBlock = 12;
	private static int YpixelsPerBlock = 9;

	// ######## CUSTOMIZATION PART ######## - end

	/*
	public static void main(String[] args) throws IOException {

		String mainImagePath = "C:/Image/main.jpg";
		String sourceFilesPath = "C:/Image/Source/";
		String collageFilePath = "C:/Image/Collage/";

		makeSourceSmaller(sourceFilesPath);
		
		String smallPicDir = sourceFilesPath + "small/";
		startCollage(mainImagePath, smallPicDir, collageFilePath);
	}*/

	public static void startCollage(String mainImagePath,
			String sourceFilesPath, String CollageFilePath) {
		
		//System.out.println("Running...");

		

		try {
			long startTime = System.currentTimeMillis();
			
			
			//resize main face image
			//File originalMainImage = new File(mainImagePath);
			File mainImage = new File(mainImagePath);
			resize(mainImage, CollageFilePath, true);
			
			//process new main face image
			
			File sourceFile2 = new File(CollageFilePath+mainImage.getName());
			//System.out.println("******" + CollageFilePath+mainImage.getName());
			
			BufferedImage mainImage2 = ImageIO.read(sourceFile2);
			int[][][] blockArray = BlockMainImage(mainImage2);

			File[] listOfSourceFiles = GetListOfSourcefiles(sourceFilesPath);

			//System.out.println("get list of source files ");
			int[][] sourceFileRGBs = ClassifySourceImages(listOfSourceFiles);

			//System.out.println("classify source images");

			boolean[] doesSourceContribute = new boolean[listOfSourceFiles.length];
			int[][] matchArray = MatchImagesWithBlocks(blockArray,
					sourceFileRGBs, doesSourceContribute);

			//System.out.println("match images with blocks");

			CreateResult(CollageFilePath, mainImage2, listOfSourceFiles,
					matchArray, doesSourceContribute);
			//System.out.println("Done!!");

			long endTime = System.currentTimeMillis();
			//System.out.println("Total execution time:"+(endTime - startTime)/60000);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void makeSourceSmaller(String sourceFilesPath, String smallPicDir) throws IOException {
		File sourceFolder = new File(sourceFilesPath);
		
		// create dir under souce dir
		if (new File(smallPicDir).mkdir()) {
			//System.out.println("small dir created");
		}

		File[] listOfFiles = sourceFolder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (!listOfFiles[i].isFile() 
					&& !listOfFiles[i].getName().endsWith("jpg")
					&& !listOfFiles[i].getName().endsWith("JPG"))
				continue;
			BufferedImage imageSource = ImageIO.read(listOfFiles[i]);
			if (imageSource == null)
				continue;
			resize(listOfFiles[i], smallPicDir, false);
		}
	}

	// BufferedImage img
	public static void resize(File image, String destDir, boolean isMain) throws IOException {
		BufferedImage img = ImageIO.read(image);

		int w = img.getWidth();
		int h = img.getHeight();

		System.out.println(w);
		System.out.println(h);
		int newW = 400;
		if(isMain){
			newW = 1000;
		}		

		double tempH = (((double) h / (double) w) * newW);

		int newH = (int) Math.floor(tempH);

		//System.out.println(newW);
		//System.out.println(newH);

		BufferedImage dimg = new BufferedImage(newW, newH, img.getType());
		Graphics2D g = dimg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
		g.dispose();

		String destFile = destDir + image.getName();
		File imageFile = new File(destFile);
		ImageIO.write(dimg, "jpg", imageFile);

		// return dimg;
	}

	public static int[][][] BlockMainImage(BufferedImage imageSource)
			throws IOException {
		int width = imageSource.getWidth();
		int height = imageSource.getHeight();

		int[][] rgbArray = new int[width][height];

		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				rgbArray[x][y] = imageSource.getRGB(x, y);

		int numberOfXBlocks = width / XpixelsPerBlock;
		int numberOfYBlocks = height / YpixelsPerBlock;

		int[][][] blockArray = new int[numberOfXBlocks][numberOfYBlocks][3];

		for (int i = 0; i < numberOfXBlocks; i++)
			for (int j = 0; j < numberOfYBlocks; j++) {
				int value, sumR = 0;
				int sumG = 0;
				int sumB = 0;

				for (int x = 0; x < XpixelsPerBlock; x++)
					for (int y = 0; y < YpixelsPerBlock; y++) {
						value = rgbArray[i * XpixelsPerBlock + x][j
								* YpixelsPerBlock + y] + 16777216;
						sumB += (value % 256);
						value = (value / 256);
						sumG += (value % 256);
						value = (value / 256);
						sumR += (value);
					}
				int resolutionPerBlock = XpixelsPerBlock * YpixelsPerBlock;
				blockArray[i][j][0] = sumR / resolutionPerBlock;
				blockArray[i][j][1] = sumG / resolutionPerBlock;
				blockArray[i][j][2] = sumB / resolutionPerBlock;
			}

		return blockArray;
	}

	public static File[] GetListOfSourcefiles(String sourceFilesPath)
			throws IOException {
		File sourceFolder = new File(sourceFilesPath);
		File[] listOfFiles = sourceFolder.listFiles();
		int sourceFileCount = 0;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (!listOfFiles[i].isFile())
				continue;
			BufferedImage imageSource = ImageIO.read(listOfFiles[i]);
			if (imageSource == null)
				continue;
			sourceFileCount++;
		}

		File[] listOfSourceFiles = new File[sourceFileCount];
		for (int i = 0, n = 0; i < listOfFiles.length; i++) {
			if (!listOfFiles[i].isFile())
				continue;
			BufferedImage imageSource = ImageIO.read(listOfFiles[i]);
			if (imageSource == null)
				continue;
			listOfSourceFiles[n++] = listOfFiles[i];
		}
		return listOfSourceFiles;
	}

	public static int[][] ClassifySourceImages(File[] listOfSourceFiles)
			throws IOException {
		int[][] sourceFileRGBs = new int[listOfSourceFiles.length][3];
		for (int i = 0; i < listOfSourceFiles.length; i++) {
			if (!listOfSourceFiles[i].isFile())
				continue;

			BufferedImage imageSource = ImageIO.read(listOfSourceFiles[i]);

			if (imageSource == null)
				continue;

			int width = imageSource.getWidth();
			int height = imageSource.getHeight();

			int value, sumR = 0;
			int sumG = 0;
			int sumB = 0;

			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++) {
					value = imageSource.getRGB(x, y) + 16777216;
					sumB += (value % 256);
					value = (value / 256);
					sumG += (value % 256);
					value = (value / 256);
					sumR += (value);
				}
			int resolution = width * height;
			sourceFileRGBs[i][0] = sumR / resolution;
			sourceFileRGBs[i][1] = sumG / resolution;
			sourceFileRGBs[i][2] = sumB / resolution;
		}
		return sourceFileRGBs;
	}

	public static int[][] MatchImagesWithBlocks(int[][][] blockArray,
			int[][] sourceFileRGBs, boolean[] doesSourceContribute) {
		int[][] matchArray = new int[blockArray.length][blockArray[0].length];

		for (int j = 0; j < blockArray.length; j++)
			for (int k = 0; k < blockArray[0].length; k++) {
				int bestDiff = -1;
				for (int i = 0; i < sourceFileRGBs.length; i++) {
					int diff = (int) Math.sqrt(Math.pow(blockArray[j][k][0]
							- sourceFileRGBs[i][0], 2)
							+ Math.pow(blockArray[j][k][1]
									- sourceFileRGBs[i][1], 2)
							+ Math.pow(blockArray[j][k][2]
									- sourceFileRGBs[i][2], 2));
					if (bestDiff == -1 || (diff < bestDiff)) {
						bestDiff = diff;
						matchArray[j][k] = i;
					}
				}
				doesSourceContribute[matchArray[j][k]] = true;
			}
		return matchArray;
	}

	public static void CreateResult(String CollageFilePath,
			BufferedImage mainImage, File[] listOfSourceFiles,
			int[][] matchArray, boolean[] doesSourceContribute)
			throws IOException {
		int[][][] shortImages = new int[doesSourceContribute.length][XpixelsPerBlock
				* enlargeSize][YpixelsPerBlock * enlargeSize];
		for (int n = 0; n < doesSourceContribute.length; n++) {
			if (!doesSourceContribute[n])
				continue;

			BufferedImage imageSource = ImageIO.read(listOfSourceFiles[n]);

			int width = imageSource.getWidth();
			int height = imageSource.getHeight();

			int[][] rgbArray = new int[width][height];

			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					rgbArray[x][y] = imageSource.getRGB(x, y);

			int shortImageXPixel = XpixelsPerBlock * enlargeSize;
			int shortImageYPixel = YpixelsPerBlock * enlargeSize;
			int XShrinkRatio = width / shortImageXPixel;
			int YShrinkRatio = height / shortImageYPixel;
			int resolution = XShrinkRatio * YShrinkRatio;

			for (int i = 0; i < shortImageXPixel; i++)
				for (int j = 0; j < shortImageYPixel; j++) {
					int value, sumR = 0;
					int sumG = 0;
					int sumB = 0;

					for (int x = 0; x < XShrinkRatio; x++)
						for (int y = 0; y < YShrinkRatio; y++) {
							value = rgbArray[i * XShrinkRatio + x][j
									* YShrinkRatio + y] + 1;
							sumB += (value % 256);
							value = (value / 256);
							sumG += (value % 256);
							value = (value / 256);
							sumR += value;
						}

					sumR /= resolution;
					sumG /= resolution;
					sumB /= resolution;
					shortImages[n][i][j] = sumR * 65536 + sumG * 256 + sumB - 1;
				}
		}

		int width = mainImage.getWidth() * enlargeSize;
		int height = mainImage.getHeight() * enlargeSize;

		BufferedImage imageResult = new BufferedImage(width, height,
				mainImage.getType());

		int XpixelPerBlockInResult = XpixelsPerBlock * enlargeSize;
		int YpixelPerBlockInResult = YpixelsPerBlock * enlargeSize;
		int XBlocks = width / XpixelPerBlockInResult;
		int YBlocks = height / YpixelPerBlockInResult;

		for (int i = 0; i < XBlocks; i++)
			for (int j = 0; j < YBlocks; j++) {
				int match = matchArray[i][j];
				for (int x = 0; x < XpixelPerBlockInResult; x++)
					for (int y = 0; y < YpixelPerBlockInResult; y++)
						imageResult.setRGB((i * XpixelPerBlockInResult + x), (j
								* YpixelPerBlockInResult + y),
								shortImages[match][x][y]);
			}

		File resultFile = new File(CollageFilePath + "Collage"+System.currentTimeMillis()+".jpg");
		ImageIO.write(imageResult, "jpg", resultFile);
	}
}