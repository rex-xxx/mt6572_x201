package com.vladium.emma.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.vladium.logging.Logger;

public class MethodFilter {
    
    
    //by MTK54039 start;added for exclude specific methods.
      public static boolean sHasMethodFilter=false;//tell other classes do we have 
                                                  //method filter or not. So they won't check
                                                  //method filter if not.
      
      private static final Logger S_LOGGER=Logger.getLogger();
      
      private static ArrayList<String> sFilterMethods=null;
      
      private static boolean sTrue4OnlyFalse4Exclude=true;//true 4 only count filter methods, false 4 count all but exclude filter methods.
      
      /**
       * get exclude methods from a config file.
       * return true, if success(means we want to exclude some methods and provided a file list them.)
       * return false, if failed, means no such file tell us to exclude the methods.
       * */
      public static boolean getExcludeMethods(File configFile){
          if(!configFile.exists()||!configFile.canRead()){
              return false;
          }
          
          FileReader fReader=null;
          try {
              fReader=new FileReader(configFile);
              BufferedReader bReader=new BufferedReader(fReader);
              String bufferString=null;
              sFilterMethods=new ArrayList<String>();
              while((bufferString=bReader.readLine())!=null){
                  if (bufferString.trim().length()==0 || bufferString.startsWith("#")) {//ignore blank line or comment
                      continue;
                  }
                  bufferString=bufferString.replaceAll(" ", "");//trim the black space, to avoid typo
                  S_LOGGER.info("excluded Methods :"+bufferString);
                  if (bufferString.startsWith("-")) {//"-" means exclude methods in the filter.
                      sTrue4OnlyFalse4Exclude=false;
                      bufferString=bufferString.substring(1);
                  }
                  else if (bufferString.startsWith("+")) {//"+" means only count methods in the filter.
                      bufferString=bufferString.substring(1);
                  }else{}//no modifier means only count methods in the filter same with '+'.
                  sFilterMethods.add(bufferString);
              }
              return true;
          } catch (IOException e) {
              S_LOGGER.info("method filter file not found, will not exclude custom method.");
              return false;
          }
      }
      /**
       * check if the method is excluded using the methods finger print.
       * */
      public static boolean isMethodExcluded(String methodSignature){
          methodSignature=methodSignature.replaceAll(" ", "");// this methodSignature is get from EMMA
          // trim the blank space, to avoid typo in the method filter config file
          if (sFilterMethods.contains(methodSignature)) {
              S_LOGGER.info("excluded method by full:"+methodSignature);
              return !sTrue4OnlyFalse4Exclude;//this flag is set while reading the method filter config file
              //if the config file contains '-' modifier, means we want to exclude the method.
              //so the flag's value is flase, if the methodSignature was contained in the filterMethods
              // return true to tell EMMA this method is excluded.
              //other wise , if no modifier or the modifier is '+', means we only want to include the methods in the filter.
              //so the flag's value is true, if the methodSignature was contained in the filterMethods
              //return false to tell EMMA this method is not excluded and other methods is excluded.
          }
          String wildCard="*"+methodSignature.substring(methodSignature.lastIndexOf('.'));
          if(sFilterMethods.contains(wildCard)){
              S_LOGGER.info("excluded method by wild:"+methodSignature);
              return !sTrue4OnlyFalse4Exclude;
          }
          return sTrue4OnlyFalse4Exclude;
      }
      
      //by MTK54039 end
}
