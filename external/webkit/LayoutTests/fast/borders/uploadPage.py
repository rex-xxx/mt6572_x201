#!/usr/bin/python

import os;
import time;
import sys;

def uploadPage(local_file):   
  start_browser_cmd = "adb shell am start -a android.intent.action.VIEW -f 0x00008000 -n com.android.browser/com.android.browser.BrowserActivity -d ";
  tmp_folder = "/mnt/sdcard/tmp";
  browser_tmp_folder = "file://"+tmp_folder;

  cmd = "adb push "+local_file+ " "+ tmp_folder+"/"+local_file;
  print cmd;
  os.system(cmd);
  
  local_file_path = browser_tmp_folder+"/"+local_file;
  cmd = start_browser_cmd+local_file_path;
  print "loading ",local_file_path;  
  print cmd;
  os.system(cmd);
    
if __name__ == "__main__": 
  
  if len(sys.argv) != 2:
    print len(sys.argv);
    print "Usage: uploadPage.py [file]\n";
    sys.exit(0);
  
  uploadPage(sys.argv[1]);       
