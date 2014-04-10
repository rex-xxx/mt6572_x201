package com.google.doclava;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MediatekStubs {
    public static void writeStubsAndApi(String stubsDir, String apiFile,
            HashSet<String> stubPackages) {
        // figure out which classes we need
        final HashSet<ClassInfo> notStrippable = new HashSet<ClassInfo>();
        ClassInfo[] all = Converter.allClasses();
        PrintStream mapiWriter = null;
        PrintStream keepListWriter = null;
        if (apiFile != null) {
            try {
                File xml = new File(apiFile);
                ClearPage.ensureDirectory(xml);
                mapiWriter = new PrintStream(new BufferedOutputStream(
                        new FileOutputStream(xml)));
            } catch (FileNotFoundException e) {
                Errors.error(Errors.IO_ERROR, new SourcePositionInfo(apiFile,
                        0, 0), "Cannot open file for write.");
            }
        }

        // Perform default class stripping process in doclava. It keeps unique
        // classes that are public or protected.
        for (ClassInfo cl : all) {
            // if (cl.checkLevel() && cl.isIncluded()) {
            // cantStripThis(cl, notStrippable, "0:0");
            // }
            Stubs.cantStripThis(cl, notStrippable, "0:0");
        }

        // Include only classes that contain @internal words.
        HashMap<PackageInfo, List<ClassInfo>> packages = new HashMap<PackageInfo, List<ClassInfo>>();
        for (ClassInfo cl : notStrippable) {
            if (cl.containInternal()) {
                // write out the stubs
                if (stubsDir != null) {
                    // TODO: Enable this will cause non-internal API to be
                    // added.
                    // Need to check why.
                    // Stubs.writeClassFile(stubsDir, notStrippable, cl);
                }

                if (mapiWriter != null) {
                    if (packages.containsKey(cl.containingPackage())) {
                        packages.get(cl.containingPackage()).add(cl);
                    } else {
                        ArrayList<ClassInfo> classes = new ArrayList<ClassInfo>();
                        classes.add(cl);
                        packages.put(cl.containingPackage(), classes);
                    }
                }
            }
        }

        if (mapiWriter != null) {
            Stubs.writeApi(mapiWriter, packages, notStrippable);
            mapiWriter.close();
        }
    }
}
