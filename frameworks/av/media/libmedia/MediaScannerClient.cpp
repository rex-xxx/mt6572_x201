/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <media/mediascanner.h>

#include <utils/StringArray.h>

#include "autodetect.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"
#ifndef ANDROID_DEFAULT_CODE
#undef LOG_TAG
#define LOG_TAG "MediaScannerClient"

struct CharRange {
    uint16_t first;
    uint16_t last;
};

#define ARRAY_SIZE(x)   (sizeof(x) / sizeof(*x))//addedby xu lai

//#include "utils/Log.h"
#endif

namespace android {

MediaScannerClient::MediaScannerClient()
    :   mNames(NULL),
        mValues(NULL),
        mLocaleEncoding(kEncodingNone)
{
#ifndef ANDROID_DEFAULT_CODE 
    ALOGI("MediaScannerClient Cons\n"); 
 #endif  
}

MediaScannerClient::~MediaScannerClient()
{
#ifndef ANDROID_DEFAULT_CODE
    ALOGI("MediaScannerClient ~Decons\n"); 
#endif   
    delete mNames;
    delete mValues;
}

void MediaScannerClient::setLocale(const char* locale)
{
#ifndef ANDROID_DEFAULT_CODE
    ALOGI("MediaScannerClient +setLocale locale:%s \n",locale);   
#endif 
    if (!locale) return;

    if (!strncmp(locale, "ja", 2))
        mLocaleEncoding = kEncodingShiftJIS;
    else if (!strncmp(locale, "ko", 2))
        mLocaleEncoding = kEncodingEUCKR;
    else if (!strncmp(locale, "zh", 2)) {
        if (!strcmp(locale, "zh_CN")) {
            // simplified chinese for mainland China
            mLocaleEncoding = kEncodingGBK;
        } else {
            // assume traditional for non-mainland Chinese locales (Taiwan, Hong Kong, Singapore)
            mLocaleEncoding = kEncodingBig5;
        }
    }
#ifndef ANDROID_DEFAULT_CODE
	else if(!strncmp(locale, "es", 2) || !strncmp(locale, "ru", 2) || !strncmp(locale, "pt", 2) \
		|| !strncmp(locale, "fr", 2) || !strncmp(locale, "de", 2) || !strncmp(locale, "tr", 2) \
		|| !strncmp(locale, "it", 2) || !strncmp(locale, "in", 2) || !strncmp(locale, "ms", 2) \
		|| !strncmp(locale, "ms", 2) || !strncmp(locale, "vi", 2) || !strncmp(locale, "ar", 2) \
		|| !strncmp(locale, "th", 2) || !strncmp(locale, "nl", 2) )
	{
		//all the lanuage is related to ISO-8859 charactor encoding
		mLocaleEncoding = kEncodingISO8859;
	}

    ALOGI("MediaScannerClient -setLocale mLocaleEncoding:%x \n",mLocaleEncoding);
#endif    
}

void MediaScannerClient::beginFile()
{
    mNames = new StringArray;
    mValues = new StringArray;
}

status_t MediaScannerClient::addStringTag(const char* name, const char* value)
{
#ifndef ANDROID_DEFAULT_CODE
   ALOGV("addStringTag mLocaleEncoding:%x \n",mLocaleEncoding);

   // validate to make sure it is legal utf8
   uint32_t valid_chars;
//   if (oscl_str_is_valid_utf8((const uint8 *)value, valid_chars))
   {   
//       if (mLocaleEncoding != kEncodingNone)
    { 
#else
    if (mLocaleEncoding != kEncodingNone) {
#endif 
       
        // don't bother caching strings that are all ASCII.
        // call handleStringTag directly instead.
        // check to see if value (which should be utf8) has any non-ASCII characters
#ifndef ANDROID_DEFAULT_CODE
           ALOGV("addStringTag 1 name:%s, value:%s \n",name,value);
#endif
        bool nonAscii = false;
        const char* chp = value;
        char ch;
        while ((ch = *chp++)) {
            if (ch & 0x80) {
                nonAscii = true;
                break;
            }
        }

        if (nonAscii) {
#ifndef ANDROID_DEFAULT_CODE
              ALOGV("addStringTag nonAscii \n"); 
#endif           
            // save the strings for later so they can be used for native encoding detection
            mNames->push_back(name);
            mValues->push_back(value);
            return OK;
        }
        // else fall through
    }

    // autodetection is not necessary, so no need to cache the values
    // pass directly to the client instead
#ifndef ANDROID_DEFAULT_CODE
       ALOGV("+handleStringTag \n");
#endif
    return handleStringTag(name, value);
}
#ifndef ANDROID_DEFAULT_CODE
/*          
   else
   {
      ALOGD( "value '%s' is not a legal UTF8 string\n",value );
      return true;
   } 
*/   

}
#endif


#ifndef ANDROID_DEFAULT_CODE
bool charMatchISO8859(uint8_t ch)
{
	if(((ch > 0x00 && ch < 0x1F)) || (ch == 0x7F) || ((ch > 0x80) && (ch < 0x9F)) )
		return false;
	return true;

}
#endif


#ifndef ANDROID_DEFAULT_CODE
//code page of sight words in GBK,added by xu lai
static const CharRange kGBKSWRanges[] = {
    { 0xB0A1, 0xB0FE },
    { 0xB1A1, 0xB1FE },
    { 0xB2A1, 0xB2FE },
    { 0xB3A1, 0xB3FE },
    { 0xB4A1, 0xB4FE },
    { 0xB5A1, 0xB5FE },
    { 0xB6A1, 0xB6FE },
    { 0xB7A1, 0xB7FE }, 
    { 0xB8A1, 0xB8FE },
    { 0xB9A1, 0xB9FE },
    { 0xBAA1, 0xBAFE },
    { 0xBBA1, 0xBBFE },
    { 0xBCA1, 0xBCFE },
    { 0xBDA1, 0xBDFE },
    { 0xBEA1, 0xBEFE }, 
    { 0xBFA1, 0xBFFE },
    
    { 0xC0A1, 0xC0FE },
    { 0xC1A1, 0xC1FE },
    { 0xC2A1, 0xC2FE },
    { 0xC3A1, 0xC3FE },
    { 0xC4A1, 0xC4FE },
    { 0xC5A1, 0xC5FE },
    { 0xC6A1, 0xC6FE },
    { 0xC7A1, 0xC7FE }, 
    { 0xC8A1, 0xC8FE },
    { 0xC9A1, 0xC9FE },
    { 0xCAA1, 0xCAFE },
    { 0xCBA1, 0xCBFE },
    { 0xCCA1, 0xCCFE },
    { 0xCDA1, 0xCDFE },
    { 0xCEA1, 0xCEFE }, 
    { 0xCFA1, 0xCFFE },
    
    { 0xD0A1, 0xD0FE },
    { 0xD1A1, 0xD1FE },
    { 0xD2A1, 0xD2FE },
    { 0xD3A1, 0xD3FE },
    { 0xD4A1, 0xD4FE },
    { 0xD5A1, 0xD5FE },
    { 0xD6A1, 0xD6FE },
    { 0xD7A1, 0xD7FE }, 
    { 0xD8A1, 0xD8FE },
    { 0xD9A1, 0xD9FE },
    { 0xDAA1, 0xDAFE },
    { 0xDBA1, 0xDBFE },
    { 0xDCA1, 0xDCFE },
    { 0xDDA1, 0xDDFE },
    { 0xDEA1, 0xDEFE }, 
    { 0xDFA1, 0xDFFE },
    
    { 0xE0A1, 0xE0FE },
    { 0xE1A1, 0xE1FE },
    { 0xE2A1, 0xE2FE },
    { 0xE3A1, 0xE3FE },
    { 0xE4A1, 0xE4FE },
    { 0xE5A1, 0xE5FE },
    { 0xE6A1, 0xE6FE },
    { 0xE7A1, 0xE7FE }, 
    { 0xE8A1, 0xE8FE },
    { 0xE9A1, 0xE9FE },
    { 0xEAA1, 0xEAFE },
    { 0xEBA1, 0xEBFE },
    { 0xECA1, 0xECFE },
    { 0xEDA1, 0xEDFE },
    { 0xEEA1, 0xEEFE }, 
    { 0xEFA1, 0xEFFE },
    
    { 0xF0A1, 0xF0FE },
    { 0xF1A1, 0xF1FE },
    { 0xF2A1, 0xF2FE },
    { 0xF3A1, 0xF3FE },
    { 0xF4A1, 0xF4FE },
    { 0xF5A1, 0xF5FE },
    { 0xF6A1, 0xF6FE },
    { 0xF7A1, 0xF7FE }, 
        
};

//code page of sight words in BIG5,added by xu lai
static const CharRange kBig5SWRanges[] = {
    { 0xA440, 0xA47E },
    { 0xA4A1, 0xA4FE },
    { 0xA540, 0xA57E },
    { 0xA5A1, 0xA5FE },
    { 0xA640, 0xA67E },
    { 0xA6A1, 0xA6FE },
    { 0xA740, 0xA77E },
    { 0xA7A1, 0xA7FE },
    { 0xA840, 0xA87E },
    { 0xA8A1, 0xA8FE },
    { 0xA940, 0xA97E },
    { 0xA9A1, 0xA9FE },
    { 0xAA40, 0xAA7E },
    { 0xAAA1, 0xAAFE },
    { 0xAB40, 0xAB7E },
    { 0xABA1, 0xABFE },
    { 0xAC40, 0xAC7E },
    { 0xACA1, 0xACFE },
    { 0xAD40, 0xAD7E },
    { 0xADA1, 0xADFE },
    { 0xAE40, 0xAE7E },
    { 0xAEA1, 0xAEFE },
    { 0xAF40, 0xAF7E },
    { 0xAFA1, 0xAFFE },
    { 0xB040, 0xB07E },
    { 0xB0A1, 0xB0FE },
    { 0xB140, 0xB17E },
    { 0xB1A1, 0xB1FE },
    { 0xB240, 0xB27E },
    { 0xB2A1, 0xB2FE },
    { 0xB340, 0xB37E },
    { 0xB3A1, 0xB3FE },
    { 0xB440, 0xB47E },
    { 0xB4A1, 0xB4FE },
    { 0xB540, 0xB57E },
    { 0xB5A1, 0xB5FE },
    { 0xB640, 0xB67E },
    { 0xB6A1, 0xB6FE },
    { 0xB740, 0xB77E },
    { 0xB7A1, 0xB7FE },
    { 0xB840, 0xB87E },
    { 0xB8A1, 0xB8FE },
    { 0xB940, 0xB97E },
    { 0xB9A1, 0xB9FE },
    { 0xBA40, 0xBA7E },
    { 0xBAA1, 0xBAFE },
    { 0xBB40, 0xBB7E },
    { 0xBBA1, 0xBBFE },
    { 0xBC40, 0xBC7E },
    { 0xBCA1, 0xBCFE },
    { 0xBD40, 0xBD7E },
    { 0xBDA1, 0xBDFE },
    { 0xBE40, 0xBE7E },
    { 0xBEA1, 0xBEFE },
    { 0xBF40, 0xBF7E },
    { 0xBFA1, 0xBFFE },
    { 0xC040, 0xC07E },
    { 0xC0A1, 0xC0FE },
    { 0xC140, 0xC17E },
    { 0xC1A1, 0xC1FE },
    { 0xC240, 0xC27E },
    { 0xC2A1, 0xC2FE },
    { 0xC340, 0xC37E },
    { 0xC3A1, 0xC3FE },
    { 0xC440, 0xC47E },
    { 0xC4A1, 0xC4FE },
    { 0xC540, 0xC57E },
    { 0xC5A1, 0xC5FE },
    { 0xC640, 0xC67E },   
};

#define ARRAY_SIZE(x)   (sizeof(x) / sizeof(*x))
static bool charMatchestest(int ch, const CharRange* encodingRanges, int rangeCount) {
    // Use binary search to see if the character is contained in the encoding
    int low = 0;
    int high = rangeCount;

    while (low < high) {
        int i = (low + high) / 2;
        const CharRange* range = &encodingRanges[i];
        if (ch >= range->first && ch <= range->last)
            return true;
        if (ch > range->last)
            low = i + 1;
        else
            high = i;
    }

    return false;
}


#endif

#ifndef ANDROID_DEFAULT_CODE
//added by xu lai for mediascanner enhance	
static bool ISUTF8(const char*s){
	int utf8ByteNumber=0;
	bool ISALLASCII=true;
	int len =strlen(s);
//	ALOGD("strlen=%d,string=%s",len,s);
	for (int i=0;i<len;i++){
		if((s[i]&0x80)==0x80){
			ISALLASCII=false;//the string is not all ASCII 
			if(utf8ByteNumber){
				if((s[i]&0xC0)!=0x80)
					return false;
				else
					utf8ByteNumber--;
			}
			else{
				if((s[i]&0xC0)==0x80)//the first utf byte cannot begin with 10
					return false;
				else{ 
					if((s[i]&0xE0)==0xC0)//the first utf byte begin with 110
						utf8ByteNumber=1;
					else
					if((s[i]&0xF0)==0xE0)//the first utf byte begin with 1110
						utf8ByteNumber=2;
					else
					if((s[i]&0xF1)==0xF0)//the first utf byte begin with 11110
						utf8ByteNumber=3;
					else 
						return false;//unicode :0~0x10FFFF
				}
			}
		}
	}
	ALOGD("string is utf8");
	return !ISALLASCII;
}
//added by xu lai for mediascanner enhance	

#endif
static uint32_t possibleEncodings(const char* s)
{

   ALOGI("+possibleEncodings %s \n",s);       
 
    uint32_t result = kEncodingAll;

    uint8_t ch1, ch2;
    uint8_t* chp = (uint8_t *)s;

 
#ifndef ANDROID_DEFAULT_CODE   
//added by xu.lai for enhance
	if(*s!=0xFF){
		if(ISUTF8(s))
			return 0xFFFFFFFF;
	}
	else
	   	s++;

	if(*chp==0xFF)
		chp++;
//added by xu.lai for enhance	
  uint32_t uiISO8859 = kEncodingISO8859;
	int GBK_count=0;
	int BIG5_count=0;


    while ((ch1 = *chp++)) {
		if(ch1&0x80){
			if(uiISO8859 && charMatchISO8859(ch1)){

						uiISO8859 &= kEncodingISO8859;	
					}
					else
						uiISO8859  = 0;
			ch2=*chp++;
			if(uiISO8859 && charMatchISO8859(ch2)){

						uiISO8859 &= kEncodingISO8859;	
					}
					else
						uiISO8859  = 0;
		     int ch = (int)ch1 << 8 | (int)ch2;
//			 ALOGD("ch %x \n",ch);
		     result &= findPossibleEncodings(ch);
//			 ALOGD("result %x \n",result);
			 if(charMatchestest(ch,kGBKSWRanges,ARRAY_SIZE(kGBKSWRanges)))
			 	GBK_count++;
			 if(charMatchestest(ch,kBig5SWRanges,ARRAY_SIZE(kBig5SWRanges)))
				BIG5_count++;
          }
    	}
        // else ASCII character, which could be anything

   result |= uiISO8859; //contain the iso8859 info in the result
//   ALOGD("result 0x%x,GBK_count %d,BIG5_count %d \n",result,GBK_count,BIG5_count);
/*
//BIG5 & GBK enhance
   if(GBK_count|BIG5_count){
		if(GBK_count>BIG5_count)
			result&=0xFFFB;
		else
			if(GBK_count<BIG5_count)				
				result&=0xFFFD;
   }
 */
   ALOGI("-possibleEncodings %d \n",result); 
#else
    while ((ch1 = *chp++)) {
        if (ch1 & 0x80) {
            ch2 = *chp++;
            ch1 = ((ch1 << 6) & 0xC0) | (ch2 & 0x3F);
            // ch1 is now the first byte of the potential native char

            ch2 = *chp++;
            if (ch2 & 0x80)
                ch2 = ((ch2 << 6) & 0xC0) | (*chp++ & 0x3F);
            // ch2 is now the second byte of the potential native char
            int ch = (int)ch1 << 8 | (int)ch2;
            result &= findPossibleEncodings(ch);
        }
        // else ASCII character, which could be anything
    }

#endif

    return result;
}


#ifndef ANDROID_DEFAULT_CODE   
void MediaScannerClient::convertValues(uint32_t encoding, int i)
{
    ALOGV("+convertValues encoding:%d \n",encoding);   
   
    const char* enc = NULL;
    switch (encoding) {
        case kEncodingShiftJIS:
            enc = "shift-jis";
            break;
        case kEncodingGBK:
            enc = "gbk";
            break;
        case kEncodingBig5:
            enc = "Big5";
            break;
        case kEncodingEUCKR:
            enc = "EUC-KR";
            break;
		case kEncodingISO8859:
			enc = NULL; // ISO8859 no need to change
			break;
        default:
            // check if the mLocaleEncoding is GBK, use GBK as first priority
            // This code is used for encoding type is not clear.
            if(encoding > 0)
            {
                if((mLocaleEncoding == kEncodingGBK) || (mLocaleEncoding == kEncodingNone))
                {
                    if(encoding & 0x2){
                       enc = "gbk";
                    }
                    else if(encoding & 0x4){
                       enc = "Big5";
                    }
                    else if(encoding & 0x8){
                       enc = "EUC-KR";
                    }                                
                    else if(encoding & 0x1){
                       enc = "shift-jis";
                    }                    
                }
                else if(mLocaleEncoding == kEncodingBig5)
                {
                    if(encoding & 0x4){
                       enc = "Big5";
                    }                
                    else if(encoding & 0x2){
                       enc = "gbk";
                    }
                    else if(encoding & 0x8){
                       enc = "EUC-KR";
                    }
                    else if(encoding & 0x1){
                       enc = "shift-jis";
                    }                     
                }
				else if(mLocaleEncoding == kEncodingISO8859)
				{
					if(encoding & 0x10){ // if iso8859 no need to convert
						enc = NULL;
					}
					else if(encoding & 0x2){
                       enc = "gbk";
                    }
                    else if(encoding & 0x4){
                       enc = "Big5";
                    }
                    else if(encoding & 0x8){
                       enc = "EUC-KR";
                    }                                
                    else if(encoding & 0x1){
                       enc = "shift-jis";
                    }     
				}
            }
            ALOGV("+convertValues mLocaleEncoding:%d, enc=%s \n",mLocaleEncoding,enc); 
    }

    if (enc) 
    {
        UErrorCode status = U_ZERO_ERROR;

        ALOGV("+convertValues enc:%s \n",enc); 

        UConverter *conv = ucnv_open(enc, &status);
        if (U_FAILURE(status)) {
            ALOGD("could not create UConverter for %s\n", enc);
            return;
        }
        UConverter *utf8Conv = ucnv_open("UTF-8", &status);
        if (U_FAILURE(status)) {
            ALOGD("could not create UConverter for UTF-8\n");
            ucnv_close(conv);
            return;
        }

        // for each value string, convert from native encoding to UTF-8
        {
          
            uint8_t* src = (uint8_t *)mValues->getEntry(i);
            int srclen=strlen((char *)src);
			if(*src==0xFF)
				src++;
			if(srclen==1)
				mValues->setEntry(i, "");
			else{
            int len = strlen((char *)src);
            uint8_t* dest = src;

            uint8_t uch;
            while ((uch = *src++)) {
			#ifndef ANDROID_DEFAULT_CODE 
				*dest++ = uch;
			#else
				 if (uch & 0x80)
                    *dest++ = ((uch << 6) & 0xC0) | (*src++ & 0x3F);
                else
					*dest++ = uch;
			#endif
            }
            *dest = 0;

            // now convert from native encoding to UTF-8
            const char* source = mValues->getEntry(i);
			if(*source==0xFF)
				source++;
            int targetLength = len * 3 + 1;
            char* buffer = new char[targetLength];
            if (!buffer)
                goto _Fail_Case;
            
            char* target = buffer;

            ucnv_convertEx(utf8Conv, conv, &target, target + targetLength,
                    &source, (const char *)dest, NULL, NULL, NULL, NULL, TRUE, TRUE, &status);

 //           ALOGV("+convertValues source:%s, target:%s, status=%d \n",source,target,status);

            if (U_FAILURE(status)) {
                ALOGD("ucnv_convertEx failed: %d\n", status);
                mValues->setEntry(i, "???");
            } else {
                // zero terminate
                *target = 0;
                mValues->setEntry(i, buffer);
            }         

            delete[] buffer;
			} 
        }
        
_Fail_Case:
        ucnv_close(conv);
        ucnv_close(utf8Conv);
    }
	else{  //if enc==0,convert metadata to UTF8 as ISO8859-1
		   const char* src = (char *)mValues->getEntry(i);
            int srclen=strlen(src);
			if(*src==0xFF)
				src++;
			if(srclen==1)
				mValues->setEntry(i, "");
			else{
				size_t utf8len = 0;
    			for (size_t i = 0; i < srclen; ++i) {
        			if (src[i] == '\0') {
            			srclen = i;
           				break;
       				 } 
					else if (src[i] < 0x80) 
           				 ++utf8len;
     				else 
            			utf8len += 2;
        				
    			}

    		if (utf8len == srclen) {
        // Only ASCII characters present.

      		 mValues->setEntry(i, src);
			ALOGD("Only ASCII characters present.");
       		 return;
   			 }

    		char *tmp = new char[utf8len];
    		char *ptr = tmp;
    		for (size_t i = 0; i < srclen; ++i) {
        		if (src[i] == '\0') {
           			 break;
       			 } else if (src[i] < 0x80) {
           			 *ptr++ = src[i];
        		 } else if (src[i] < 0xc0) {
           			 *ptr++ = 0xc2;
           			 *ptr++ = src[i];
        		} else {
           			 *ptr++ = 0xc3;
           			 *ptr++ = src[i] - 64;
        		}
    		}

    		mValues->setEntry(i, tmp);
            ALOGD("encoding=%d,convert ISO8859-1 to UTF-8",encoding);
    		delete[] tmp;
    		tmp = NULL;
		}
	}


    ALOGV("-convertValues \n");
    
}
#else

void MediaScannerClient::convertValues(uint32_t encoding)
{
    const char* enc = NULL;
    switch (encoding) {
        case kEncodingShiftJIS:
            enc = "shift-jis";
            break;
        case kEncodingGBK:
            enc = "gbk";
            break;
        case kEncodingBig5:
            enc = "Big5";
            break;
        case kEncodingEUCKR:
            enc = "EUC-KR";
            break;
    }

    if (enc) {
        UErrorCode status = U_ZERO_ERROR;

        UConverter *conv = ucnv_open(enc, &status);
        if (U_FAILURE(status)) {
            ALOGE("could not create UConverter for %s", enc);
            return;
        }
        UConverter *utf8Conv = ucnv_open("UTF-8", &status);
        if (U_FAILURE(status)) {
            ALOGE("could not create UConverter for UTF-8");
            ucnv_close(conv);
            return;
        }

        // for each value string, convert from native encoding to UTF-8
        for (int i = 0; i < mNames->size(); i++) {
            // first we need to untangle the utf8 and convert it back to the original bytes
            // since we are reducing the length of the string, we can do this in place
            uint8_t* src = (uint8_t *)mValues->getEntry(i);
            int len = strlen((char *)src);
            uint8_t* dest = src;

            uint8_t uch;
            while ((uch = *src++)) {
                if (uch & 0x80)
                    *dest++ = ((uch << 6) & 0xC0) | (*src++ & 0x3F);
                else
                    *dest++ = uch;
            }
            *dest = 0;

            // now convert from native encoding to UTF-8
            const char* source = mValues->getEntry(i);
            int targetLength = len * 3 + 1;
            char* buffer = new char[targetLength];
            // don't normally check for NULL, but in this case targetLength may be large
            if (!buffer)
                break;
            char* target = buffer;

            ucnv_convertEx(utf8Conv, conv, &target, target + targetLength,
                    &source, (const char *)dest, NULL, NULL, NULL, NULL, TRUE, TRUE, &status);
            if (U_FAILURE(status)) {
                ALOGE("ucnv_convertEx failed: %d", status);
                mValues->setEntry(i, "???");
            } else {
                // zero terminate
                *target = 0;
                mValues->setEntry(i, buffer);
            }

            delete[] buffer;
        }

        ucnv_close(conv);
        ucnv_close(utf8Conv);
    }
}
#endif

void MediaScannerClient::endFile()
{
#ifndef ANDROID_DEFAULT_CODE   
   ALOGV("endFile mLocaleEncoding:%d \n",mLocaleEncoding);   
    {      
#else
    if (mLocaleEncoding != kEncodingNone) {
#endif         
        int size = mNames->size();
        uint32_t encoding = kEncodingAll;

#ifndef ANDROID_DEFAULT_CODE   
        
        ALOGV("endFile +possibleEncodings size: %d \n",size);     
        //// compute a bit mask containing all possible encodings
        for (int i = 0; i < mNames->size(); i++)
        { 
        	char const*s=mValues->getEntry(i);
		//	if((s[0]==0xFF)|(!ISUTF8(s))){
            encoding = possibleEncodings(s);
            ALOGD("endFile +possibleEncodings: %d \n",encoding);    
			if(!(encoding==0xFFFFFFFF))
            convertValues(encoding,i);
		//}
			
        }

#else
        // compute a bit mask containing all possible encodings
        for (int i = 0; i < mNames->size(); i++)
            encoding &= possibleEncodings(mValues->getEntry(i));

        // if the locale encoding matches, then assume we have a native encoding.
        if (encoding & mLocaleEncoding)
            convertValues(mLocaleEncoding);
#endif

        // finally, push all name/value pairs to the client
        for (int i = 0; i < mNames->size(); i++) {
            status_t status = handleStringTag(mNames->getEntry(i), mValues->getEntry(i));
            if (status) {
                break;
            }
        }
    }
    // else addStringTag() has done all the work so we have nothing to do

    delete mNames;
    delete mValues;
    mNames = NULL;
    mValues = NULL;
}

}  // namespace android
