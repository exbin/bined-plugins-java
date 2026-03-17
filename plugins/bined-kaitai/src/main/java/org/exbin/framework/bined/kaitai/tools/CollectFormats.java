/*
 * Copyright (C) ExBin Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exbin.framework.bined.kaitai.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Kaitai settings options.
 */
@ParametersAreNonnullByDefault
public class CollectFormats {

    public static final String ROOT_PATH = "src/main/resources/org/exbin/framework/bined/kaitai/resources/formats";
    public static final Map<String, String> dirDetails = new HashMap<>();

    static {
        dirDetails.put("3d", "3D Models");
        dirDetails.put("archive", "Archive Files");
        dirDetails.put("cad", "CAD");
        dirDetails.put("common", "Commonly Used Data Types");
        dirDetails.put("database", "Databases");
        dirDetails.put("executable", "Executables and Byte-code");
        dirDetails.put("filesystem", "Filesystems");
        dirDetails.put("firmware", "Firmware");
        dirDetails.put("font", "Fonts");
        dirDetails.put("game", "Game Data Files");
        dirDetails.put("geospatial", "Geospatial (Maps)");
        dirDetails.put("hardware", "Hardware Protocols");
        dirDetails.put("image", "Image Files");
        dirDetails.put("log", "Logs");
        dirDetails.put("macos", "macOS-specific");
        dirDetails.put("machine_code", "CPU / Machine Code Disassembly");
        dirDetails.put("media", "Multimedia Files");
        dirDetails.put("network", "Networking Protocols");
        dirDetails.put("scientific", "Scientific Applications");
        dirDetails.put("security", "Security");
        dirDetails.put("serialization", "Serialization Protocols");
        dirDetails.put("windows", "Windows-specific");
    }

    public static void main(String[] args) {
        collectFormats();
    }

    public static void collectFormats() {
        File outputFile = new File(ROOT_PATH + "/" + "formats.txt");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(outputFile);
            OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");

            File rootDirectory = new File(ROOT_PATH);
            processDir(out, rootDirectory, "");

            out.close();
        } catch (IOException ex) {
            Logger.getLogger(CollectFormats.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void processDir(OutputStreamWriter out, File directory, String prefix) throws IOException {
        List<String> subDirs = new ArrayList<>();
        for (File file : directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        })) {
            subDirs.add(file.getName());
        }
        Collections.sort(subDirs);
        
        for (String dirName : subDirs) {
            File file = new File(directory, dirName);
            out.write(prefix + dirName + "/\n");
            String detail = dirDetails.get(prefix + dirName);
            out.write(detail == null ? "" : detail);
            out.write("\n");
            
            processDir(out, file, prefix + file.getName() + "/");
        }

        List<String> subFiles = new ArrayList<>();
        for (File file : directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        })) {
            String fileName = file.getName();
            if (fileName.endsWith(".ksy")) {
                subFiles.add(fileName);
            }
        }
        Collections.sort(subFiles);
        
        for (String fileName : subFiles) {
            String title = "";

            File file = new File(directory, fileName);
            FileInputStream stream = null;
            BufferedReader reader = null;
            try {
                stream = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                StringBuilder builder = new StringBuilder();
                String read;
                while ((read = reader.readLine()) != null) {
                    int pos = read.indexOf("title:");
                    if (pos >= 0) {
                        title = read.substring(pos + 6).trim();
                        if ('\"' == title.charAt(0)) {
                            title = title.substring(1, title.length() - 1);
                        }
                        break;
                    }
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }

            out.write(prefix + fileName + "\n");
            out.write(title);
            out.write("\n");
        }
    }
}
