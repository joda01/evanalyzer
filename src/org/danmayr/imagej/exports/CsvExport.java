package org.danmayr.imagej.exports;

import ij.*;

import java.io.*;
import java.util.*;

import org.danmayr.imagej.algorithm.structs.*;

public class CsvExport {
    CsvExport() {

    }

    public static void Export(String outputFolder, FolderResults results) {
        String outPut = "";
        for (Map.Entry<String, Folder> entry : results.getFolders().entrySet()) {
            String key = entry.getKey();
            outPut += key;
            outPut += "\n";

            Folder folder = entry.getValue();
            for (Map.Entry<String, Image> entry1 : folder.getImages().entrySet()) {
                String imgName = entry1.getKey();
                outPut += imgName;
                outPut += "\n";

                Image image = entry1.getValue();
                for (Map.Entry<Integer, Channel> entry2 : image.getChannels().entrySet()) {
                    int chName = entry2.getKey();
                    Channel channel = entry2.getValue();
                    outPut += chName;
                    outPut += "\n";
                    outPut += "\n";

                    for (Map.Entry<Integer, ParticleInfo> entry3 : channel.getRois().entrySet()) {
                        int chNr = entry3.getKey();
                        ParticleInfo info = entry3.getValue();
                        outPut += info.toString() + "\n";
                    }
                }
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFolder + "/" + "result.csv"));
            writer.write(outPut);

            writer.close();
        } catch (IOException io) {

        }
    }
}
