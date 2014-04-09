package com.mediatek.gallery3d.ext;

import java.util.List;

public class MovieExtension implements IMovieExtension {

    @Override
    public List<Integer> getFeatureList() {
        return null;
    }

    @Override
    public IMovieStrategy getMovieStrategy() {
        return new MovieStrategy();
    }
    
    @Override
    public IActivityHooker getHooker() {
        return null;
    }
}
