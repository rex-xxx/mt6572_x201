//
// MTK theme management class.
//
#ifndef __LIBS_MTKTHEMEMANAGER_H
#define __LIBS_MTKTHEMEMANAGER_H

#include <utils/String8.h>
#include <utils/threads.h>

namespace android {

class MTKThemeManager {

public:
    static MTKThemeManager* getInstance();
    static void releaseInstance();
    bool isFileInMap(const char* apkName, const char* fileName);
    void parseThemeMapIfNeeded();

private:
    MTKThemeManager();
    virtual ~MTKThemeManager();

    struct ThemeFileList {
        const char *fileName;
    };

    struct ThemePathMap {
        const char *path;
        ThemeFileList *fileList;
        int fileCount;

        ThemePathMap() {
            path = NULL;
            fileList = NULL;
            fileCount = 0;
        }
    };

    static int mModuleCount;
    ThemePathMap *mPathMap;
    static MTKThemeManager *mInstance;

private:
    static char* THEME_MAP_FILE;

private:
    bool findFileInList(ThemeFileList *fileList, int listLen, const char* fileName);
    void dumpMapInfo();
};  // end of MTKThemeManager

}; // namespace android

#endif // __LIBS_ASSETMANAGER_H
