package net.kdt.pojavlaunch.tasks;

import com.ashmeet.hyperlauncher.utils.Architecture;
import com.ashmeet.hyperlauncher.utils.Tools;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.value.ExtractSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NativesExtractor {
    private static final ArrayList<String> LIBRARY_BLACKLIST = createLibraryBlacklist();
    private final File mDestinationDir;
    private final String mLibraryLocation;

    public NativesExtractor(File mDestinationDir) {
        this.mDestinationDir = mDestinationDir;
        this.mLibraryLocation = "jni/"+getAarArchitectureName()+"/";
    }


    private static ArrayList<String> createLibraryBlacklist() {
        String[] includedLibraryNames = new File(Tools.NATIVE_LIB_DIR).list();
        ArrayList<String> blacklist = new ArrayList<>(includedLibraryNames.length);
        for(String libraryName : includedLibraryNames) {

            if(libraryName.equals("libjnidispatch.so")) continue;
            blacklist.add(libraryName);
        }
        blacklist.trimToSize();
        return blacklist;
    }

    private static String getAarArchitectureName() {
        int architecture = Architecture.getDeviceArchitecture();
        switch (architecture) {
            case Architecture.ARCH_ARM:
                return "armeabi-v7a";
            case Architecture.ARCH_ARM64:
                return "arm64-v8a";
            case Architecture.ARCH_X86:
                return "x86";
            case Architecture.ARCH_X86_64:
                return "x86_64";
        }
        throw new RuntimeException("Unknown CPU architecture: "+architecture);
    }

    private void extract(File source, ExtractFilter extractFilter) throws IOException {
        byte[] buffer = new byte[8192];
        try (FileInputStream fileInputStream = new FileInputStream(source);
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {


            NonCloseableInputStream entryCopyStream = new NonCloseableInputStream(zipInputStream);
            ZipEntry entry;
            while((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if(!extractFilter.shouldExtract(entryName) || entry.isDirectory()) continue;

                entryName = FileUtils.getFileName(entryName);

                if(entryName == null || LIBRARY_BLACKLIST.contains(entryName)) continue;

                processEntry(entryCopyStream, entry, new File(mDestinationDir, entryName), buffer);
            }
        }
    }

    public void extractFromAar(File source) throws IOException {
        extract(source, name->name.startsWith(mLibraryLocation));
    }

    public void extractMoJson(File source, ExtractSettings settings) throws IOException {
        extract(source, name->{
            if(settings.exclude == null) return true;
            for(String exclude : settings.exclude) {
                if(name.startsWith(exclude)) return false;
            }
            return true;
        });
    }

    private static long fileCrc32(File target, byte[] buffer) throws IOException {
        try(FileInputStream fileInputStream = new FileInputStream(target)) {
            CRC32 crc32 = new CRC32();
            int len;
            while((len = fileInputStream.read(buffer)) != -1) {
                crc32.update(buffer, 0, len);
            }
            return crc32.getValue();
        }
    }

    private void processEntry(InputStream sourceStream, ZipEntry zipEntry, File entryDestination, byte[] buffer) throws IOException {
        if(entryDestination.exists()) {
            long expectedSize = zipEntry.getSize();
            long expectedCrc32 = zipEntry.getCrc();
            long realSize = entryDestination.length();
            long realCrc32 = fileCrc32(entryDestination, buffer);

            if(realSize == expectedSize && realCrc32 == expectedCrc32) return;
        }

        org.apache.commons.io.FileUtils.copyInputStreamToFile(sourceStream, entryDestination);
    }

    private interface ExtractFilter {
        boolean shouldExtract(String entry);
    }

    private static class NonCloseableInputStream extends FilterInputStream {

        protected NonCloseableInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {

        }
    }
}
