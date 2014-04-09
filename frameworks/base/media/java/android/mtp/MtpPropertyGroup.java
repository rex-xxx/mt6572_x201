/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.mtp;

import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import java.util.ArrayList;

/// M: Added for USB Develpment debug, more log for more debuging help @{
import com.mediatek.xlog.SXlog;
import com.mediatek.xlog.Xlog;
import java.lang.Integer;
/// M: @}

class MtpPropertyGroup {

    private static final String TAG = "MtpPropertyGroup";

    private class Property {
        // MTP property code
        int     code;
        // MTP data type
        int     type;
        // column index for our query
        int     column;

        Property(int code, int type, int column) {
            this.code = code;
            this.type = type;
            this.column = column;
        }
    }

    private final MtpDatabase mDatabase;
    private final IContentProvider mProvider;
    private final String mVolumeName;
    private final Uri mUri;
    /// M: Added Modification for ALPS00255822, bug from WHQL test
    private String mPathHandelDepthQuery;

    // list of all properties in this group
    private final Property[]    mProperties;

    // list of columns for database query
    private String[]             mColumns;

    private static final String ID_WHERE = Files.FileColumns._ID + "=?";
    private static final String FORMAT_WHERE = Files.FileColumns.FORMAT + "=?";
    private static final String ID_FORMAT_WHERE = ID_WHERE + " AND " + FORMAT_WHERE;
    private static final String PARENT_WHERE = Files.FileColumns.PARENT + "=?";
    private static final String PARENT_FORMAT_WHERE = PARENT_WHERE + " AND " + FORMAT_WHERE;
    /// M: Added Modification for ALPS00255822, bug from WHQL test @{
    //private static final String DEPTH_DATA_WHERE = Files.FileColumns.DATA + "=?";
    private static final String DEPTH_DATA_WHERE = Files.FileColumns.DATA;
    private static final String ID_DEPTH_DATA_WHERE = ID_WHERE + " AND " + DEPTH_DATA_WHERE;
    /// M: @}
    // constructs a property group for a list of properties
    public MtpPropertyGroup(MtpDatabase database, IContentProvider provider, String volume,
            int[] properties) {
        mDatabase = database;
        mProvider = provider;
        mVolumeName = volume;
        mUri = Files.getMtpObjectsUri(volume);

        int count = properties.length;
        ArrayList<String> columns = new ArrayList<String>(count);
        columns.add(Files.FileColumns._ID);

        mProperties = new Property[count];
        for (int i = 0; i < count; i++) {
            mProperties[i] = createProperty(properties[i], columns);
        }
        count = columns.size();
        mColumns = new String[count];
        for (int i = 0; i < count; i++) {
            mColumns[i] = columns.get(i);
        }
    }

    private Property createProperty(int code, ArrayList<String> columns) {
        String column = null;
        int type;

         switch (code) {
            case MtpConstants.PROPERTY_STORAGE_ID:
                column = Files.FileColumns.STORAGE_ID;
                type = MtpConstants.TYPE_UINT32;
                break;
             case MtpConstants.PROPERTY_OBJECT_FORMAT:
                column = Files.FileColumns.FORMAT;
                type = MtpConstants.TYPE_UINT16;
                break;
            case MtpConstants.PROPERTY_PROTECTION_STATUS:
                // protection status is always 0
                type = MtpConstants.TYPE_UINT16;
                break;
            case MtpConstants.PROPERTY_OBJECT_SIZE:
                column = Files.FileColumns.SIZE;
                type = MtpConstants.TYPE_UINT64;
                break;
            case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
                column = Files.FileColumns.DATA;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_NAME:
                column = MediaColumns.TITLE;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_DATE_MODIFIED:
                column = Files.FileColumns.DATE_MODIFIED;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_DATE_ADDED:
                column = Files.FileColumns.DATE_ADDED;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE:
                column = Audio.AudioColumns.YEAR;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_PARENT_OBJECT:
                column = Files.FileColumns.PARENT;
                type = MtpConstants.TYPE_UINT32;
                break;
            case MtpConstants.PROPERTY_PERSISTENT_UID:
                // PUID is concatenation of storageID and object handle
                column = Files.FileColumns.STORAGE_ID;
                type = MtpConstants.TYPE_UINT128;
                break;
            case MtpConstants.PROPERTY_DURATION:
                column = Audio.AudioColumns.DURATION;
                type = MtpConstants.TYPE_UINT32;
                break;
            case MtpConstants.PROPERTY_TRACK:
                column = Audio.AudioColumns.TRACK;
                type = MtpConstants.TYPE_UINT16;
                break;
            case MtpConstants.PROPERTY_DISPLAY_NAME:
                column = MediaColumns.DISPLAY_NAME;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_ARTIST:
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_ALBUM_NAME:
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_ALBUM_ARTIST:
                column = Audio.AudioColumns.ALBUM_ARTIST;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_GENRE:
                // genre requires a special query
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_COMPOSER:
                column = Audio.AudioColumns.COMPOSER;
                type = MtpConstants.TYPE_STR;
                break;
            case MtpConstants.PROPERTY_DESCRIPTION:
                column = Images.ImageColumns.DESCRIPTION;
                type = MtpConstants.TYPE_STR;
                break;
            default:
                type = MtpConstants.TYPE_UNDEFINED;
                Log.e(TAG, "unsupported property " + code);
                break;
        }

        /// M: Added Modification for ALPS00255822, bug from WHQL test
        SXlog.d(TAG, "createProperty: code = 0x" + Integer.toHexString(code) + ", type = 0x" + Integer.toHexString(type));

        if (column != null) {
            columns.add(column);
            return new Property(code, type, columns.size() - 1);
        } else {
            return new Property(code, type, -1);
        }
    }

   private String queryString(int id, String column) {
        Cursor c = null;
        try {
            // for now we are only reading properties from the "objects" table
            c = mProvider.query(mUri,
                            new String [] { Files.FileColumns._ID, column },
                            ID_WHERE, new String[] { Integer.toString(id) }, null, null);
            if (c != null && c.moveToNext()) {
                return c.getString(1);
            } else {
                return "";
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String queryAudio(int id, String column) {
        Cursor c = null;
        try {
            c = mProvider.query(Audio.Media.getContentUri(mVolumeName),
                            new String [] { Files.FileColumns._ID, column },
                            ID_WHERE, new String[] { Integer.toString(id) }, null, null);
            if (c != null && c.moveToNext()) {
                return c.getString(1);
            } else {
                return "";
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private String queryGenre(int id) {
        Cursor c = null;
        try {
            Uri uri = Audio.Genres.getContentUriForAudioId(mVolumeName, id);
            c = mProvider.query(uri,
                            new String [] { Files.FileColumns._ID, Audio.GenresColumns.NAME },
                            null, null, null, null);
            if (c != null && c.moveToNext()) {
                return c.getString(1);
            } else {
                return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "queryGenre exception", e);
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private Long queryLong(int id, String column) {
        Cursor c = null;
        try {
            // for now we are only reading properties from the "objects" table
            c = mProvider.query(mUri,
                            new String [] { Files.FileColumns._ID, column },
                            ID_WHERE, new String[] { Integer.toString(id) }, null, null);
            if (c != null && c.moveToNext()) {
                return new Long(c.getLong(1));
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }

    private static String nameFromPath(String path) {
        // extract name from full path
        int start = 0;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            start = lastSlash + 1;
        }
        int end = path.length();
        if (end - start > 255) {
            end = start + 255;
        }
        return path.substring(start, end);
    }

    MtpPropertyList getPropertyList(int handle, int format, int depth) {
        Log.d(TAG, "getPropertyList handle: " + Integer.toHexString(handle) + ", format: " + Integer.toHexString(format) + ", depth: " + Integer.toHexString(depth));
        if (depth > 1) {
            // we only support depth 0 and 1
            // depth 0: single object, depth 1: immediate children
            return new MtpPropertyList(0, MtpConstants.RESPONSE_SPECIFICATION_BY_DEPTH_UNSUPPORTED);
        }

        String where;
        String[] whereArgs;
        if (format == 0) {
            if (handle == 0xFFFFFFFF) {
                // select all objects
                where = null;
                whereArgs = null;
            } else {
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                if(handle == 0x00 && depth == 0xFFFFFFFF) {
                    //select all object from root -> select all object
                    SXlog.d(TAG, "getPropertyList handle: 0x" + Integer.toHexString(handle) + ", depth: 0x" + Integer.toHexString(depth) + ", select all object!!");
                    where = null;
                    whereArgs = null;
                } else {
                /// M: @}
                whereArgs = new String[] { Integer.toString(handle) };
                if (depth == 1) {
                    where = PARENT_WHERE;
                } 
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                /*
                else if(depth == 0xFFFFFFFF && handle!=0)
                {
                    boolean test = getFilePathWithHandle(handle);
                    SXlog.d(TAG, "getPropertyList handle: 0x" + Integer.toHexString(handle) + ", depth: 0x" + Integer.toHexString(depth) + ", test: " + test);
                    SXlog.d(TAG, "getPropertyList mPathHandelDepthQuery: " + mPathHandelDepthQuery);
                    SXlog.d(TAG, "getPropertyList mPathHandelDepthQuery.length: " + mPathHandelDepthQuery.length());

                    if(mPathHandelDepthQuery.length() > 0)
                    {                
                        //    "_data like '/mnt/sdcard/A/%' or _data = '/mnt/sdcard/A' "
                        
                        String end = new String("/%");
                        String temp1 = new String("\"");
                        String temp2 = new String("'");
                        String begin = DEPTH_DATA_WHERE;
                        String pathHandle = mPathHandelDepthQuery;
                        String like = new String("like");
                        String path1;
                        
                        path1 = temp1.concat(begin);

                        SXlog.d(TAG, "getPropertyList stage 1: path1: " + path1);

                        mPathHandelDepthQuery = begin.concat(" like " +temp2+pathHandle + end +temp2+" or "+begin+" = " + temp2 + pathHandle+temp2+" ");

                        SXlog.d(TAG, "getPropertyList stage 2: mPathHandelDepthQuery: " + mPathHandelDepthQuery);

                        mPathHandelDepthQuery = mPathHandelDepthQuery.concat(temp1);
                        
                        SXlog.d(TAG, "getPropertyList handle: 0x" + Integer.toHexString(handle) + ", depth: 0x" + Integer.toHexString(depth));
                        SXlog.d(TAG, "getPropertyList stage 3: mPathHandelDepthQuery: " + mPathHandelDepthQuery);
                            
                        where = mPathHandelDepthQuery;
                        whereArgs = null;
                    }
                    else
                        where = ID_WHERE;
                    
                }
                */
                /// M: @}
                else {
                    where = ID_WHERE;
                }
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                }
                /// M: @}
            }
        } else {
            if (handle == 0xFFFFFFFF) {
                // select all objects with given format
                where = FORMAT_WHERE;
                whereArgs = new String[] { Integer.toString(format) };
            } else {
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                if(handle == 0x00 && depth == 0xFFFFFFFF) {
                    //equal to handle == 0xFFFFFFFF, MTP spec p.252, // select all objects with given format
                    SXlog.d(TAG, "getPropertyList handle: 0x" + Integer.toHexString(handle) + ", depth: 0x" + Integer.toHexString(depth) + ", select all object with given format!!");
                    /*change the handle for fix the next operation*/
                    handle = 0xFFFFFFFF;
                    where = FORMAT_WHERE;
                    whereArgs = new String[] { Integer.toString(format) };
                } else {
                /// M: @}
                    whereArgs = new String[] { Integer.toString(handle), Integer.toString(format) };
                    if (depth == 1) {
                        where = PARENT_FORMAT_WHERE;
                    } else {
                        where = ID_FORMAT_WHERE;
                    }
                /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                }
                /// M: @}
            }
        }

        Cursor c = null;
        try {
            // don't query if not necessary
            if (depth > 0 || handle == 0xFFFFFFFF || mColumns.length > 1) {
                /// M: Added Modification for ALPS00255822, bug from WHQL test
                SXlog.d(TAG, "getPropertyList handle: 0x" + Integer.toHexString(handle) + ", depth: 0x" + Integer.toHexString(depth) + ", don't query if not necessary!!");
                c = mProvider.query(mUri, mColumns, where, whereArgs, null, null);
                if (c == null) {
                    return new MtpPropertyList(0, MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                }
            }
            /// M: Added Modification for ALPS00255822, bug from WHQL test @{
            // Special case of depth = -1 with handle id
            // don't query if not necessary
            else if (depth < 0 && handle > 0) {
                Xlog.d(TAG, "getPropertyList handle: 0x" + Integer.toHexString(handle) + ", depth: 0x" + Integer.toHexString(depth) + ", depth < 0 && handle > 0!!");
                c = mProvider.query(mUri, mColumns, where, whereArgs, null, null);
                if (c == null) {
                    return new MtpPropertyList(0, MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                }
            }
            /// M: @}

            int count = (c == null ? 1 : c.getCount());
            MtpPropertyList result = new MtpPropertyList(count * mProperties.length,
                    MtpConstants.RESPONSE_OK);

            // iterate over all objects in the query
            for (int objectIndex = 0; objectIndex < count; objectIndex++) {
                if (c != null) {
                    c.moveToNext();
                    handle = (int)c.getLong(0);
                }

                // iterate over all properties in the query for the given object
                for (int propertyIndex = 0; propertyIndex < mProperties.length; propertyIndex++) {
                    Property property = mProperties[propertyIndex];
                    int propertyCode = property.code;
                    int column = property.column;

                    // handle some special cases
                    switch (propertyCode) {
                        case MtpConstants.PROPERTY_PROTECTION_STATUS:
                            // protection status is always 0
                            result.append(handle, propertyCode, MtpConstants.TYPE_UINT16, 0);
                            break;
                        case MtpConstants.PROPERTY_OBJECT_FILE_NAME:
                            // special case - need to extract file name from full path
                            String value = c.getString(column);
                            if (value != null) {
                                result.append(handle, propertyCode, nameFromPath(value));
                            } else {
                                result.setResult(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                            }
                            break;
                        case MtpConstants.PROPERTY_NAME:
                            // first try title
                            String name = c.getString(column);
                            // then try name
                            if (name == null) {
                                name = queryString(handle, Audio.PlaylistsColumns.NAME);
                            }
                            // if title and name fail, extract name from full path
                            if (name == null) {
                                name = queryString(handle, Files.FileColumns.DATA);
                                if (name != null) {
                                    name = nameFromPath(name);
                                }
                            }
                            if (name != null) {
                                result.append(handle, propertyCode, name);
                            } else {
                                result.setResult(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                            }
                            break;
                        case MtpConstants.PROPERTY_DATE_MODIFIED:
                        case MtpConstants.PROPERTY_DATE_ADDED:
                            // convert from seconds to DateTime
                            /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                            int seconds = c.getInt(column);
 			    Log.w(TAG, "getPropertyList handle: 0x" + Integer.toHexString(handle) + ", c.getInt(column) = " + seconds);
                            /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                            result.append(handle, propertyCode, format_date_time(c.getInt(column)));
                            break;
                        case MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE:
                            // release date is stored internally as just the year
                            int year = c.getInt(column);
                            /// M: Added Modification for ALPS00255822, bug from WHQL test @{
                            //String dateTime = Integer.toString(year) + "0101T000000";
                            String dateTime = "00000101T000000";   //no year
                            /// M: @}
                            result.append(handle, propertyCode, dateTime);
                            break;
                        case MtpConstants.PROPERTY_PERSISTENT_UID:
                            // PUID is concatenation of storageID and object handle
                            long puid = c.getLong(column);
                            puid <<= 32;
                            puid += handle;
                            result.append(handle, propertyCode, MtpConstants.TYPE_UINT128, puid);
                            break;
                        case MtpConstants.PROPERTY_TRACK:
                            result.append(handle, propertyCode, MtpConstants.TYPE_UINT16,
                                        c.getInt(column) % 1000);
                            break;
                        case MtpConstants.PROPERTY_ARTIST:
                            result.append(handle, propertyCode,
                                    queryAudio(handle, Audio.AudioColumns.ARTIST));
                            break;
                        case MtpConstants.PROPERTY_ALBUM_NAME:
                            result.append(handle, propertyCode,
                                    queryAudio(handle, Audio.AudioColumns.ALBUM));
                            break;
                        case MtpConstants.PROPERTY_GENRE:
                            String genre = queryGenre(handle);
                            if (genre != null) {
                                result.append(handle, propertyCode, genre);
                            } else {
                                result.setResult(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                            }
                            break;
                        default:
                            if (property.type == MtpConstants.TYPE_STR) {
                                result.append(handle, propertyCode, c.getString(column));
                            } else if (property.type == MtpConstants.TYPE_UNDEFINED) {
                                result.append(handle, propertyCode, property.type, 0);
                            } else {
                                result.append(handle, propertyCode, property.type,
                                        c.getLong(column));
                            }
                            break;
                    }
                }
            }

            return result;
        } catch (RemoteException e) {
            return new MtpPropertyList(0, MtpConstants.RESPONSE_GENERAL_ERROR);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        // impossible to get here, so no return statement
    }

    /// M: Added Modification for ALPS00255822, bug from WHQL test to get the path. @{
    private boolean getFilePathWithHandle(int handle) {
        SXlog.d(TAG, "getFilePathWithHandle handle = " + Integer.toHexString(handle));
        final String[] PATH_SIZE_FORMAT_PROJECTION = new String[] {
                Files.FileColumns._ID, // 0
                Files.FileColumns.DATA, // 1
                Files.FileColumns.SIZE, // 2
                Files.FileColumns.FORMAT, // 3
        };

        Cursor c = null;
        try {
            c = mProvider.query(mUri, PATH_SIZE_FORMAT_PROJECTION,
                            ID_WHERE, new String[] {  Integer.toString(handle) }, null, null);
            if (c != null && c.moveToNext()) {
                mPathHandelDepthQuery = c.getString(1);

                SXlog.d(TAG, "getFilePathWithHandle mPathHandelDepthQuery = " + mPathHandelDepthQuery);
                SXlog.d(TAG, "getFilePathWithHandle mPathHandelDepthQuery.length = " + mPathHandelDepthQuery.length());

                return true;

            } else {
                return false;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getObjectFilePath", e);
            return false;
        } finally {
            if (c != null) {
                c.close();
            }
            return false;
        }
    }
    /// M: @}

    private native String format_date_time(long seconds);
}
