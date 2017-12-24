package com.rajapps.jude.musicsymbols;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Sampler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    private File imageFile;
    ImageView imageview1,imageview2,imageview3,imageview4;
    TextView textview2,textview3,textview4,textview5;
    int[] ValueArr={0,0,0,0,0,0,0,0,0};//{heigth,width,black,white,perimeter,vertInter,HorizInter,DiagPos,DiagNeg}

    final int PIC_CROP = 2;
    final int Camera_Capture =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intialize();
    }

    public void intialize(){

        imageview1= (ImageView)findViewById(R.id.imageview1);
        imageview2= (ImageView)findViewById(R.id.imageview2);
        imageview3= (ImageView)findViewById(R.id.imageview3);
        imageview4= (ImageView)findViewById(R.id.imageview4);
        textview2= (TextView)findViewById(R.id.textview2);
        textview3= (TextView)findViewById(R.id.textview3);
        textview4= (TextView)findViewById(R.id.textview4);
        textview5= (TextView)findViewById(R.id.textview5); // for symbol definitions
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void process(View view){
        Intent intent =new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"test.jpg");
        Uri tempuri=Uri.fromFile(imageFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tempuri);
        //imageview1.setImageURI(tempuri);  //sets image view to image
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1); //sets video quality to high
        startActivityForResult(intent,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==Camera_Capture){
            switch(resultCode){
                case Activity.RESULT_OK:
                    if (imageFile.exists()){
                        //Toast.makeText(this, "This file was saved at "+imageFile.getAbsolutePath(),Toast.LENGTH_LONG).show();

                        imageFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"test.jpg");
                        Uri tempuri=Uri.fromFile(imageFile);

                        performCrop(tempuri);
                    }

                    else{
                        Toast.makeText(this,"There was an error saving the file",Toast.LENGTH_LONG).show();
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    break;
                default:
                    break;
            }
        }
        else if(requestCode == PIC_CROP){
            switch(resultCode) {
                case Activity.RESULT_OK:
                Bundle extras = data.getExtras();//get the returned data
                Bitmap thePic = extras.getParcelable("data");//get the cropped bitmap
                //imageview1.setImageBitmap(thePic);//display the returned cropped image

            thePic= createBlackAndWhite(thePic);
            //imageview2.setImageBitmap(thePic);//display the returned cropped image

            thePic= AutoCrop(thePic);
            imageview3.setImageBitmap(thePic);//display the returned cropped image

            thePic= EdgeDetect(thePic);
            //imageview4.setImageBitmap(thePic);

            thePic=InterSectionCount(thePic);
            //imageview4.setImageBitmap(thePic);

            //printStats(ValueArr);

            textview4.setText(getSymbolValues(ValueArr));

            textview5.setText(getSymbolDef(getSymbolValues(ValueArr)));
                case Activity.RESULT_CANCELED:
                    break;
            }
            //save to file after editing
            /*FileOutputStream out = null;
            try {
                out = new FileOutputStream(imageFile.getAbsolutePath());  //saving cropped bitmap as png
                thePic.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
        }
    }



    private void performCrop(Uri picUri){
        try {

            Intent cropIntent = new Intent("com.android.camera.action.CROP"); //call the standard crop action intent (the user device may not support it)
            cropIntent.setDataAndType(picUri, "image/*");//indicate image type and Uri
            cropIntent.putExtra("crop", "true");//set crop properties
            cropIntent.putExtra("return-data", true); //retrieve data on return
            startActivityForResult(cropIntent, PIC_CROP);//start the activity - we handle returning in onActivityResult
        }
        catch(ActivityNotFoundException anfe){
            //display an error message
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public static Bitmap createBlackAndWhite(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
        int A, R, G, B;
        int pixel;

        // scan through all pixels
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                pixel = src.getPixel(x, y); // get pixel color
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);

                if (gray > 100)// use 128 as threshold, above -> white, below -> black
                    gray = 255;
                else
                    gray = 0;
                bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));                // set new pixel color to output bitmap
            }
        }
        return bmOut;
    }

    public Bitmap AutoCrop(Bitmap src){ //takes black and white image and crops to smallest image
        int Srcwidth = src.getWidth(); //width of original bitmap
        int Srcheight = src.getHeight(); //height of original bitmap
        int outWidth, outHeight; //heigth and width of output bitmap
        int pixel = 0;
        int xStart=0, xEnd=0, yStart=0, yEnd=0;

        //vertical search from top left
        for (int x = 0; x < Srcwidth-1; ++x) {
            for (int y = 0; y < Srcheight-1; ++y) {
                pixel = src.getPixel(x, y);
                if (pixel==-16777216){ // if pixel is black //used to be 255
                    xStart =x; //starting x of image
                    break;
                }
            }
            if (pixel == -16777216){//break out of both loops
                break;
            }
        }
        //vertical search from top right
        for (int x = Srcwidth-1; x > 0; --x) { //was x>0
            for (int y = 0; y < Srcheight; ++y) { //was y < Srcheight
                pixel = src.getPixel(x, y);
                if (pixel==-16777216){ // if pixel is black
                    xEnd = x; //ending x of image
                    break;
                }
            }
            if (pixel == -16777216){//break out of both loops
                break;
            }
        }
        //horizontal search from top left
        for (int y = 0; y < Srcheight; ++y) {
            for (int x = 0; x < Srcwidth; ++x) {
                pixel = src.getPixel(x, y);
                if (pixel==-16777216){ // if pixel is black
                    yStart =y; //starting y of image
                    break;
                }
            }
            if (pixel == -16777216){//break out of both loops
                break;
            }
        }
        //horizontal search from bottom left
        for (int y = Srcheight-1; y > 0; --y) {
            for (int x = 0; x < Srcwidth;++x) {
                pixel = src.getPixel(x, y);
                if (pixel==-16777216){ // if pixel is black
                    yEnd =y; //starting y of image
                    break;
                }
            }
            if (pixel == -16777216){//break out of both loops
                break;
            }
        }
        outHeight = yEnd-yStart; //calculating new height
        outWidth =  xEnd - xStart; //calculating new width

        Bitmap btmOut= Bitmap.createBitmap(src,xStart, yStart, outWidth,outHeight);

        int blackPix=0;
        int whitePix=0;

        for (int x = 0; x < outWidth-1; ++x) { //counting pixels
            for (int y = 0; y < outHeight-1; ++y) {
                pixel = btmOut.getPixel(x, y);
                if (pixel==-16777216){ // if pixel is black //used to be 255
                    blackPix++;
                }
                if (pixel==-1){
                    whitePix++;
                }
            }
        }

        //textview2.setText("Original Height= "+Srcheight+"\nOriginal Width= "+Srcwidth+"\n\nNew Heigth= "+outHeight+"\nNew Width= "+outWidth
        //        +"\n\nBlack Pixels= "+blackPix+"\nWhite Pixels= "+ whitePix);
        //setting values into array
        ValueArr[0]=outHeight;
        ValueArr[1]=outWidth;
        ValueArr[2]=blackPix;
        ValueArr[3]=whitePix;
        return btmOut;
    }

    public Bitmap EdgeDetect(Bitmap src){
        int Srcwidth = src.getWidth(); //width of original bitmap
        int Srcheight = src.getHeight(); //height of original bitmap
        int pixel;
        int tmp;
        Bitmap btmOut=Bitmap.createBitmap(Srcwidth,Srcheight,src.getConfig());

        //vertical search from top left
        for (int x = 0; x < Srcwidth-1; ++x) {
            for (int y = 0; y < Srcheight-1; ++y) {
                pixel = src.getPixel(x, y);
                if(y!=0){
                    tmp=src.getPixel(x,y-1);
                    if(pixel!=tmp){
                        btmOut.setPixel(x, y, 0xffff0000);//red -65536
                    }
                }
            }
        }
        //horizontal search from top left
        for (int y = 0; y < Srcheight-1; ++y) {
            for (int x = 0; x < Srcwidth-1; ++x) {
                pixel = src.getPixel(x, y);
                if(x!=0){
                    tmp=src.getPixel(x-1,y);
                    if(pixel!=tmp){
                        btmOut.setPixel(x,y,0xffff0000);//red -65536
                    }
                }
            }
        }

        int redPix=0;
        for (int x = 0; x < Srcwidth-1; ++x) { //counting pixels
            for (int y = 0; y < Srcheight-1; ++y) {
                pixel = btmOut.getPixel(x, y);
                if (pixel==0xffff0000){ // if pixel is black //used to be 255
                    redPix++;
                }
            }
        }
        //textview4.setText("Perimeter Pixels= "+ redPix);
        ValueArr[4]=redPix;
        return btmOut ;
    }

    public Bitmap InterSectionCount(Bitmap src){
        int Srcwidth = src.getWidth(); //width of original bitmap
        int Srcheight = src.getHeight(); //height of original bitmap
        int pixel = 0;
        int midWidth=Srcwidth/2;
        int midHeigth=Srcheight/2;
        int vertInter=0,horizInter=0;

        for (int y = 0; y < Srcheight-1; ++y){  //vertical
            pixel=src.getPixel(midWidth, y);
            if(pixel==0xffff0000){
                vertInter++;
            }
            src.setPixel(midWidth,y,0xffff0000);//set red
        }
        for (int x = 0; x < Srcwidth-1; ++x){  //horizontal
            pixel=src.getPixel(x,midHeigth);
            if(pixel==-65536){
                horizInter++;
            }
            src.setPixel(x,midHeigth,0xffff0000);//set red
        }

        ValueArr[5]=vertInter;
        ValueArr[6]=horizInter;
        return src;
    }

    public void printStats(int[] ValueArr){
        textview2.setText("\nNew Heigth= "+ValueArr[0]+"\nNew Width= "+ValueArr[1]
                +"\n\nBlack Pixels= "+ ValueArr[2]+"\nWhite Pixels= "+ ValueArr[3]
                + "\n\nPerimeter Pixels= "+ ValueArr[4]+"\n\nVertical Intersections= "+ValueArr[5]
                +"\nHorizontal Intersections= "+ ValueArr[6]);
    }

    public String getSymbolValues(int[] ValueArr){
        int Height=ValueArr[0];
        int Width=ValueArr[1];
        int blackPix=ValueArr[2];
        int whitePix=ValueArr[3];
        int perimeter=ValueArr[4];
        int vertInter=ValueArr[5];
        int horizInter= ValueArr[6];

        double HWRatio= (double)Height/Width;
        double BWRatio= (double)blackPix/whitePix;
        double HWPRatio= (double)HWRatio/perimeter;
        String[] SymbolVals;

        int[] bestMatch={0,0};// symbol ID number, number of tests passed

        double RealHWmin,RealHWmax,RealBWmin,RealBWmax,RealHWPmin,RealHWPmax,RealVertmin,RealVertmax,RealHorizmin,RealHorizmax;
        String tmp;
        String SymbolName="";
        int NumRef= R.array.A;
        //Resources res = getResources();

        for(int x=R.array.A; x< (NumRef+26);x++) { //needs changing when adding symbols
            Resources res = getResources();
            SymbolVals = res.getStringArray(x);

            tmp=SymbolVals[0];
            RealHWmin=Double.parseDouble(tmp);
            tmp=SymbolVals[1];
            RealHWmax=Double.parseDouble(tmp);
            tmp=SymbolVals[2];
            RealBWmin=Double.parseDouble(tmp);
            tmp=SymbolVals[3];
            RealBWmax=Double.parseDouble(tmp);
            tmp=SymbolVals[4];
            RealHWPmin=Double.parseDouble(tmp);
            tmp=SymbolVals[5];
            RealHWPmax=Double.parseDouble(tmp);
            tmp=SymbolVals[6];
            RealVertmin=Double.parseDouble(tmp);
            tmp=SymbolVals[7];
            RealVertmax=Double.parseDouble(tmp);
            tmp=SymbolVals[8];
            RealHorizmin=Double.parseDouble(tmp);
            tmp=SymbolVals[9];
            RealHorizmax=Double.parseDouble(tmp);

            int testResult=0;
            if((HWRatio>= RealHWmin)&&(HWRatio<= RealHWmax)){
                testResult++;
            }
            if((BWRatio>= RealBWmin)&&(BWRatio<=RealBWmax)){
                testResult++;
            }
            if((HWPRatio>= RealHWPmin)&&(HWPRatio<=RealHWPmax)){
                testResult++;
            }
            if((vertInter>= RealVertmin)&&(vertInter<=RealVertmax)){
                testResult++;
            }
            if((horizInter>= RealHorizmin)&&(horizInter<=RealHorizmax)){
                testResult++;
            }
            else;

            //textview3.setText("Tests passed ="+testResult);

            if ((testResult>=3)&&(testResult > bestMatch[1])){ //store location of symbol with best test results
                bestMatch[1]=testResult;
                bestMatch[0]=x;//x=ID number
            }
        }

        if(bestMatch[0]!=0) {
            Resources res = getResources();
            SymbolVals = res.getStringArray(bestMatch[0]);
            SymbolName = SymbolVals[10];
        }
        else{
            SymbolName="Symbol Not Recognized";
        }
        return SymbolName;
    }

    public String getSymbolDef(String Symbol){
        String def="";
        String[] SymbolDefs;
        int NumRef= R.array.A;

        for(int x=R.array.A; x< (NumRef+14);x++) { //needs changing when adding symbols was 12
            Resources res = getResources();
            SymbolDefs = res.getStringArray(x);

            if(Symbol.equals(SymbolDefs[10])) {
                def= SymbolDefs[11];
                break;
            }
        }
        return def;
    }


}


