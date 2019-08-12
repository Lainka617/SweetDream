package com.example.sweetdream.util;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    /**
     * s
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String getCharset(String fileName) throws IOException{
        String charset;
        FileInputStream fis = new FileInputStream(fileName);
        byte[] buf = new byte[4096];
        // (1)
        UniversalDetector detector = new UniversalDetector(null);
        // (2)
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        // (3)
        detector.dataEnd();
        // (4)
        charset = detector.getDetectedCharset();
        // (5)
        detector.reset();
        return charset;
    }

    /**
     *
     * @param pathandname
     * @return
     */
    public static String getFileName(String pathandname){
        int start=pathandname.lastIndexOf("/");
        int end=pathandname.lastIndexOf(".");
        if(start!=-1 && end!=-1){
            return pathandname.substring(start+1,end);
        }else{
            return "";
        }

    }

    public static  List<File> getSuffixFile(String filePath, String suffere){
        List<File> files = new ArrayList<>();
        File f = new File(filePath);
        return getSuffixFile(files,f,suffere);
    }

    /**
     *
     * @param files
     * @param f
     * @param suffere
     * @return
     */
    public static  List<File> getSuffixFile(List<File> files, File f, final String suffere) {
        if (!f.exists()) {
            return null;
        }

        File[] subFiles = f.listFiles();
        for (File subFile : subFiles) {
            if (subFile.isHidden()){
                continue;
            }
            if(subFile.isDirectory()){
                getSuffixFile(files, subFile, suffere);
            }else if(subFile.getName().endsWith(suffere)){
                files.add(subFile);
            } else{

            }
//            Log.e("filename",subFile.getName());
        }
        return files;
    }

}
