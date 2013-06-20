package com.stahlnow.dislocator;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.overlay.Marker;

import android.app.Application;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class Bones {
	private static final String TAG = "Bones";
	
	public static class Bone {
		public final String name;
		public final String description;
		public final LatLong remote;
		public final LatLong local;
		public final Marker markerRemote;
		public final Marker markerLocal;

		public Bone(String name, String description, LatLong remote, LatLong local, Marker markerRemote, Marker markerLocal) {
			
			this.name = name;
			this.description = description;
			this.remote = remote;
			this.local = local;
			this.markerRemote = markerRemote;
			this.markerLocal = markerLocal;
		}
			

		@Override
		public String toString() {
			return this.name + "," + this.description + "," + this.local.latitude + "," + this.local.longitude + "," + this.remote.latitude + "," + this.remote.longitude;
		}
	}

	/**
	 * A map of bones, by name.
	 */
	public static final Map<String, Bone> BONE_MAP = new HashMap<String, Bone>();

	/**
	 * An array of bones (how cool is that)
	 */
	public static final List<Bone> BONES = new ArrayList<Bone>();

	public static void addItem(Bone item, boolean writeToFile) {
		BONES.add(item);
		BONE_MAP.put(item.name, item);
		if (writeToFile)
			writeData(item.toString());
	}
	
	public static void removeAllItems() {
		BONES.clear();
	}
	
	public static void removeItem(Bone item) {
		
		// remove from list
		BONES.remove(item);
		BONE_MAP.remove(item.name);
		
		// remove from map
		DislocatorActivity.localMap.getLayerManager().getLayers().remove(item.markerLocal);
		DislocatorActivity.remoteMap.getLayerManager().getLayers().remove(item.markerRemote);
		
		// remove from "bones.txt"
		String root = Environment.getExternalStorageDirectory().toString();
        File dir = new File(root + "/Dislocator");
        if (!dir.exists())
        	dir.mkdirs();
        File file = new File (dir, "bones.txt");
        File temp = new File (dir, "temp.txt");


		try {
			PrintWriter csvWriter = new  PrintWriter(new FileWriter(temp, true));
		    Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
			    String line = scanner.nextLine();
			    if (!line.startsWith(item.name + "," + item.description)) {
		            csvWriter.print(line);
		            csvWriter.print("\r\n");
			    }
			}
			
			csvWriter.close();
			
		} catch(Exception e) { 
		    Log.e(TAG, e.toString());
		} 
		
		file.delete();			// delete existing "bones.txt"
		temp.renameTo(file);	// rename temp file to "bones.txt"
		
		
	}
	
	
	public static boolean exportKML() {
		PrintWriter kmlWriterRemote;
		PrintWriter kmlWriterLocal;
        try
        {

        	String root = Environment.getExternalStorageDirectory().toString();
            File dir = new File(root + "/Dislocator");
            if (!dir.exists())
            	dir.mkdirs();
            
            File remote = new File (dir, "remote.kml");
            File local = new File(dir, "local.kml");
            
            if (!remote.exists()) {
            	remote.createNewFile();
            }
            
            if (!local.exists()) {
            	local.createNewFile();
            }
            
            kmlWriterRemote = new  PrintWriter(new FileWriter(remote, false));
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" ");
            xml.append("xmlns:gx=\"http://www.google.com/kml/ext/2.2\" ");
            xml.append("xmlns:kml=\"http://www.opengis.net/kml/2.2\" ");
            xml.append("xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
            xml.append("<Document>\n");
            xml.append("<name>").append("Remote").append("</name>\n");
            
            
            File file = new File (dir, "bones.txt");
            //PrintWriter csvWriter = new  PrintWriter(new FileWriter(file, true));
		    Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
			    String bone = scanner.nextLine();
			    List<String> bones = Arrays.asList(bone.split(","));
			    
			    xml.append("<Placemark>\n");
            	xml.append("<name>").append(bones.get(0)).append("</name>\n");
            	xml.append("<description>").append(bones.get(1)).append("</description>\n");
            	xml.append("<Point>\n");
            	xml.append("<coordinates>").append(bones.get(5)).append(",").append(bones.get(4)).append(",").append("0").append("</coordinates>\n");
            	xml.append("</Point>\n");
            	xml.append("</Placemark>\n");
			    
			}
			//csvWriter.close();
            
            
            
            
            xml.append("</Document>\n</kml>\n");
            kmlWriterRemote.write(xml.toString());
            kmlWriterRemote.close();

            // local kml
            kmlWriterLocal = new  PrintWriter(new FileWriter(local, false));
            StringBuilder xml2 = new StringBuilder();
            xml2.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml2.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" ");
            xml2.append("xmlns:gx=\"http://www.google.com/kml/ext/2.2\" ");
            xml2.append("xmlns:kml=\"http://www.opengis.net/kml/2.2\" ");
            xml2.append("xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
            xml2.append("<Document>\n");
            xml2.append("<name>").append("Local").append("</name>\n");
           
            
            File file2 = new File (dir, "bones.txt");
            //PrintWriter csvWriter2 = new  PrintWriter(new FileWriter(file2, true));
		    Scanner scanner2 = new Scanner(file2);
			while (scanner2.hasNextLine()) {
			    String bone = scanner2.nextLine();
			    List<String> bones = Arrays.asList(bone.split(","));
            
            	xml2.append("<Placemark>\n");
            	xml2.append("<name>").append(bones.get(0)).append("</name>\n");
            	xml2.append("<description>").append(bones.get(1)).append("</description>\n");
            	xml2.append("<Point>\n");
            	xml2.append("<coordinates>").append(bones.get(3)).append(",").append(bones.get(2)).append(",").append("0").append("</coordinates>\n");
            	xml2.append("</Point>\n");
            	xml2.append("</Placemark>\n");
            }
			//csvWriter2.close();
			
            xml2.append("</Document>\n</kml>\n");
            kmlWriterLocal.write(xml2.toString());
            kmlWriterLocal.close();

            return (!kmlWriterLocal.checkError() && !kmlWriterRemote.checkError());
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return false;
	}
	
	/**
	 * write to text file
	 * @param data comma separated data
	 */
	private static void writeData(String data)
    {

        PrintWriter csvWriter;
        try
        {

        	String root = Environment.getExternalStorageDirectory().toString();
            File dir = new File(root + "/Dislocator");
            if (!dir.exists())
            	dir.mkdirs();
            
            File file = new File (dir, "bones.txt");
            
            csvWriter = new  PrintWriter(new FileWriter(file, true));

            csvWriter.print(data);
            csvWriter.print("\r\n");

            csvWriter.close();


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
	
}
