package com.stahlnow.android.dislocator;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.ClusterManager;
import com.stahlnow.android.dislocator.model.Bone;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class MyBoneWriter {

    private static final String TAG = MyBoneWriter.class.getSimpleName();

    //public static boolean saveKMLForB(boolean combined, String filename)
    public static boolean saveKML(File file, Collection<Bone> items, String KMLDocumentName) throws IOException {
        PrintWriter kmlWriter;

        kmlWriter = new PrintWriter(new FileWriter(file, false));
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" ");
        xml.append("xmlns:gx=\"http://www.google.com/kml/ext/2.2\" ");
        xml.append("xmlns:kml=\"http://www.opengis.net/kml/2.2\" ");
        xml.append("xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("<Document>\n");
        xml.append("<name>").append(KMLDocumentName).append("</name>\n");

        for (Bone bone : items)
        {
            xml.append("<Placemark>\n");
            if (bone.getName() != null)
                xml.append("<name>").append(bone.getName()).append("</name>\n");
            if (bone.getSnippet() != null)
                xml.append("<description>").append(bone.getSnippet()).append("</description>\n");
            if (bone.getAddress() != null)
                xml.append("<address>").append(bone.getAddress()).append("</address>\n");
            if (bone.getPosition() != null) {
                xml.append("<Point>\n");
                xml.append("<coordinates>").append(bone.getPosition().longitude).append(",").append(bone.getPosition().latitude).append(",").append("0").append("</coordinates>\n");
                xml.append("</Point>\n");
            }
            xml.append("</Placemark>\n");

        }

        xml.append("</Document>\n</kml>\n");
        kmlWriter.write(xml.toString());
        kmlWriter.close();


        return (!kmlWriter.checkError());
    }
}
