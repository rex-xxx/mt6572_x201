/*
 * Copyright 2012 Sebastian Annies, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.mp4parser.authoring.container.mp4;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TrackBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Mp4TrackImpl;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
/// M: add log
import android.util.Log;
/**
 * Shortcut to build a movie from an MP4 file.
 */
public class MovieCreator {
    /// M: add log
    private static final String TAG="mp4parser";
    public static Movie build(ReadableByteChannel channel) throws IOException {
        IsoFile isoFile = new IsoFile(channel);
        Movie m = new Movie();

/// M: check movie box@{
	if (isoFile.getMovieBox() == null) {
            Log.w(TAG, "getMovieBox is null");
	    return null;
	} 
/// @}
        List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
        for (TrackBox trackBox : trackBoxes) {
/// M: error handle @{
            Mp4TrackImpl track = new Mp4TrackImpl(trackBox);
	    if (!track.isReady()) {
                 Log.w(TAG, "track is not ready");
                 continue;
	    }
            m.addTrack(track);
/// M: @}
            //m.addTrack(new Mp4TrackImpl(trackBox));
        }
        return m;
    }
}
