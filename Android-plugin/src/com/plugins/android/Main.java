package com.plugins.android;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.zip.*;

import com.android.tools.r8.*;
import com.*;
import com.util.*;

import javax.tools.*;
import com.plugins.android.apksign.*;

public class Main extends MMMMInterface {
    private final ArrayList<String> newJava = new ArrayList<>();
    private final ArrayList<String> procNewJava = new ArrayList<>();

    private final List<String> jnis = new ArrayList<>();
    private final HashMap<File, String> dexesOfJars = new HashMap<>();
    // private final File mPluginFolder = new File(MMMMWorkSpace, "plugins/" +
    // name());

    public static File home = new File(System.getProperty("user.home"));
    public static File aapt2 = new File("/home/mustafa/private/NoAnyId/aapt2/aapt2-9.4.1-15978811-linux/aapt2");
    public static File adb = new File("/usr/bin/adb");
    private static File androidJar = new File("/home/mustafa/Programs/android-sdk/platforms/android-10/android.jar");
    String cp;

    String processorPathes;
    String processorPathesCp;

    // private static final String JAVA = ".java";
    // private static final String USAGE = "E: Usage:\n\tjava -jar JDC.jar
    // 8683\n\t8683 is the port\n\tto stop send --stop to the server";
    private static final String NO_COMPILER_ERROR = "[ERROR] No system Java compiler available - run this with a JDK (Java Development Kit), not a JRE (Java Runtime Enviroment).";

    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public final PluginApkSigner pluginApkSigner;

    public static void main(String[] args) {
        System.out.println("built");
    }

    public Main() {
        super();
        pluginApkSigner = new PluginApkSigner(this);
    }

    public void clean() throws Exception {
        if (build != null) {
            UtilFile.delete(build);
        }
    }

    public void keystorePath(String keystorePath) throws Exception {
        pluginApkSigner.keystorePath = keystorePath;
        pluginApkSigner.keystore = new File(keystorePath);
    }

    public void alias(String alias) throws Exception {
        pluginApkSigner.alias = alias;
    }

    public void storePass(String storePass) throws Exception {
        pluginApkSigner.storePass = storePass;
    }

    public void keyPass(String keyPass) throws Exception {
        pluginApkSigner.keyPass = keyPass;
    }

    public void keystore(String keystorePath, String alias, String storePass, String keyPass)
            throws Exception {
        pluginApkSigner.keystorePath = keystorePath;
        pluginApkSigner.keystore = new File(keystorePath);
        pluginApkSigner.alias = alias;
        pluginApkSigner.storePass = storePass;
        pluginApkSigner.keyPass = keyPass;
    }

    File manifest;
    String pkgName;

    public void manifest(String manifest) throws Exception {
        this.manifest = new File(manifest);
        Utils.safty(manifest, true, Utils.SAFTY_MODE_MAKE);
        pkgName = getPackageFromAarManifest(this.manifest.getParentFile());
    }

    File res;

    public void res(String res) throws Exception {
        this.res = new File(res);
        Utils.safty(res, false, Utils.SAFTY_MODE_MAKE);
    }

    File flat;

    public void flat(String flat) throws Exception {
        this.flat = new File(flat);
        Utils.safty(flat, false, Utils.SAFTY_MODE_MAKE);
    }

    public File flat() {
        return flat;
    }

    public void setApkOut(String unsignedApkFile) throws Exception {
        pluginApkSigner.unsignedApkFile = new File(unsignedApkFile);
        Utils.safty(pluginApkSigner.unsignedApkFile.getParentFile().getPath(), false, Utils.SAFTY_MODE_MAKE);
    }

    File assets;

    public void assets(String assets) throws Exception {
        this.assets = new File(assets);
        Utils.safty(assets, false, Utils.SAFTY_MODE_MAKE);
    }

    File genarated;

    public void genarated(String genarated) throws Exception {
        this.genarated = new File(genarated);
        Utils.safty(genarated, false, Utils.SAFTY_MODE_MAKE);
    }

    public List<File> resFolders = new ArrayList<>();

    public void add_compile_res(File res) throws Exception {
        resFolders.add(res);
    }

    private void use_compiled_res(ArrayList<String> cmd) throws Exception {
        // File[] flatFolders = flat.listFiles();
        /*
         * if (flatFolders == null)
         * throw new RuntimeException(
         * "unable to load flat folders:");
         */
        // File[] flatFolders = flat.listFiles();

        for (File resFolder : resFolders) {
            File resFolderOut = new File(flat, resFolder.getParentFile().getName());
            String resFolderName = resFolderOut.getName();

            boolean canCompile = true;
            // check cache
            if (!resFolderOut.exists()) {
                resFolderOut.mkdirs();
            } else {
                canCompile = false;
            }
            File[] resFolderData = resFolder.listFiles();
            if (resFolderData == null) {
                System.out.println("Unable to process resFolder ->" + resFolderName);
                canCompile = false;
            }
            if (resFolderData.length <= 0) {
                System.out.println("Empty resFolder ->" + resFolderName);
                canCompile = false;
            }
            if (canCompile || resFolder.equals(res)) {
                System.out.println("🧳🧳COMPILING-XML(" + resFolderName + ")");
                try {
                    run(aapt2.getPath(),
                            "compile",
                            "--dir",
                            resFolder.getPath(),
                            "-o",
                            resFolderOut.getPath());
                    System.out.println("🚀🚀Compiled resFolder ->" + resFolderName);
                } catch (Exception e) {
                    throw new Exception("-❌❌Error at compiling resFolder-> {" + resFolderName + "}--- " + e);
                }
            } else {
                System.out.println(
                        "    ⚠️⚠️⚠️⚠️CAN-NOT-COMPILE-XML(" + resFolderName + ") " + resFolderOut + " is found");
            }

            String pkgName = getPackageFromAarManifest(resFolder.getParentFile());
            if (pkgName != null) {
                cmd.add("--extra-packages");
                cmd.add(pkgName);
            }

            File[] flats = resFolderOut.listFiles();
            if (flats == null)
                System.out.println("❌❌❌Unable to process flats out res for ->" + resFolderOut.getName());
            for (File f : flats) {
                cmd.add("-R");
                cmd.add(f.getPath());
            }
        }
    }

    // 1. اقرا كل قيم المانيفست
    public static Map<String, String> readManifest(File jarFile) {
        Map<String, String> map = new HashMap<>();
        try (ZipFile zip = new ZipFile(jarFile)) {
            ZipEntry manifestEntry = zip.getEntry("META-INF/MANIFEST.MF");
            if (manifestEntry == null)
                return map;

            try (InputStream is = zip.getInputStream(manifestEntry)) {
                Manifest manifest = new Manifest(is);
                Attributes attrs = manifest.getMainAttributes();

                for (Map.Entry<Object, Object> e : attrs.entrySet()) {
                    map.put(e.getKey().toString(), e.getValue().toString());
                }
            }
        } catch (Exception e) {
            return null;
        }
        return map;
    }

    public static boolean isValidJar(File jarFile) {
        Map<String, String> manifest = readManifest(jarFile);
        if (manifest == null)
            return true;
        if (manifest.containsKey("Implementation-Title") && (!manifest.containsKey("Multi-Release")
                || manifest.get("Multi-Release") == null || manifest.get("Multi-Release").equals(false)))
            return false;
        return true;
    }

    public void build_style(boolean verbose) throws Exception {
        add_compile_res(res);
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(aapt2.getPath());
        cmd.add("link");
        cmd.add("-I");
        cmd.add(androidJar.getPath());
        cmd.add("--manifest");
        cmd.add(manifest.getPath());

        cmd.add("--package-id");
        cmd.add("0x7f");

        // Add every .flat file with its own -R
        use_compiled_res(cmd);

        cmd.add("-A");
        cmd.add(assets.getPath());

        cmd.add("-o");
        cmd.add(pluginApkSigner.unsignedApkFile.getPath());
        cmd.add("--java");
        cmd.add(genarated.getPath());
        cmd.add("--auto-add-overlay");
        // cmd.add("--no-resource-deduping");
        // cmd.add("--no-version-vectors");
        // cmd.add("--no-version-transitions");
        if (verbose)
            cmd.add("-v");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        System.out.println("🧳🧳🧳linking xml... (" + resFolders.size() + ")");
        Process p = pb.start();

        // System.out.println output
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[aapt2]:" + line);
        }

        int code = p.waitFor();
        if (code != 0)
            throw new RuntimeException("aapt2 link failed: " + code);
        System.out.println("🔋🔋🔋xml linked...");
        add_java_folder(genarated.getPath());
    }

    public void add_jar(String jar) throws Exception {
        File jarFile = new File(jar);
        Utils.safty(jarFile, true);
        if (!isValidJar(jarFile)) {
            System.out.println("JAR!==🚫🚫" + jarFile.getName());
            return;
        } else {

        }

        File jar_out = new File(new File(build, "dexedJars"), path2id(jar));
        Utils.safty(jar_out, false, Utils.SAFTY_MODE_MAKE);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>"+jar_out.getName());
        
        if (cp == null) {
            cp = jar;
        } else {
            cp += System.getProperty("path.separator") + jar;
        }

        System.out.println("jar addeed successfully" + jarFile);
        if (new File(jar_out, "classes.dex").exists()) {
            System.out.println("Jar-USED-CACH⚠️==={" + new File(jar).getName() + "}");
        } else {
            dex(jar, jar_out.getPath());
        }
        // dexesOfJars.add(new File(jar_out, "classes.dex"));
        dexesOfJars.put(new File(jar_out, "classes.dex"), jar);
    }

    public static String path2id(String fullPath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(fullPath.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString(); // take first 8 chars: a3f9c2d1
        } catch (Exception e) {
            return "00000000";
        }
    }

    public void add_processor(String jar) throws Exception {
        Utils.safty(jar, true);

        System.out.println("JAR==🔋🔋🔋" + UtilFile.getNameNoExtension(jar));
        // System.out.println("-++++++++++++Cp-Proccessor===" + processorPathes);
        if (processorPathes == null) {
            processorPathes = jar;
        } else {
            processorPathes += System.getProperty("path.separator") + jar;
        }
    }

    public void add_processor_cp(String jar) throws Exception {
        Utils.safty(jar, true);
        System.out.println("JAR==🔋🔋🔋" + UtilFile.getNameNoExtension(jar));
        // System.out.println("-++++++++++++Cp-Proccessor===" + processorPathes);
        if (processorPathesCp == null) {
            processorPathesCp = jar;
        } else {
            processorPathesCp += System.getProperty("path.separator") + jar;
        }
    }

    public void add_processors_dir(String folderPath) throws Exception {
        File folder = new File(folderPath);
        Utils.safty(folder, false);

        for (File jarFile : folder.listFiles()) {
            if (jarFile.getName().endsWith(".jar")) {
                add_processor(jarFile.getPath());
            }
        }
        System.out.println("Jar filse inside " + folder.getName() + " will be processsed");
    }

    public void add_processors_cp_dir(String folderPath) throws Exception {
        File folder = new File(folderPath);
        Utils.safty(folder, false);

        add_jar_folder(folderPath);
        for (File jarFile : folder.listFiles()) {
            if (jarFile.getName().endsWith(".jar")) {
                add_processor_cp(jarFile.getPath());
            }
        }
        System.out.println("Jar filse inside " + folder.getName() + " will be processsed");
    }

    public void add_jar_folder(String folderPath) throws Exception {
        File folder = new File(folderPath);
        Utils.safty(folder, false);

        for (File jarFile : folder.listFiles()) {
            if (jarFile.getName().endsWith(".jar"))
                add_jar(jarFile.getPath());
        }
        System.out.println("Jar filse inside " + folder.getName() + " will be dexed");
    }

    public void add_java_folder(String folder_path) throws Exception {
        File java_folder = new File(folder_path);
        Utils.safty(java_folder, false);
        collectJavaFiles(java_folder, newJava);
    }

    public void add_java_folder_proc(String folder_path) throws Exception {
        File java_folder = new File(folder_path);
        Utils.safty(java_folder, false);
        add_java_folder(folder_path);
        // File generatedJava = new File(build, "generatedJava");
        // TODO
        // if (cache.markIfUpdated(folder_path, CACHE_PROC_J, procNewJava)) {
        // java_proc(generatedJava);
        // } else {
        // System.out.println("Processor CACHE");
        // add_java_folder(generatedJava.getPath());
        // }
    }

    public static void collectJavaFiles(File folder, List<String> dst) {
        // invoker safe listFiles() of null
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                collectJavaFiles(f, dst);
            } else if (f.getName().endsWith(".java")) {
                dst.add(f.getPath());
            }
        }
    }

    public void java_proc(File generatedJava) throws Exception {
        String CP = (processorPathesCp == null ? "" : "" + System.getProperty("path.separator") + processorPathesCp);

        String PROCESSORS = (processorPathes == null ? ""
                : "" + System.getProperty("path.separator") + processorPathes);

        if (PROCESSORS.trim().length() > 0) {
            System.out.println("-------------Processing-------------->");

            UtilFile.dir(generatedJava.getPath());
            List<String> cmd = new ArrayList<>();
            cmd.add("-processorpath");
            cmd.add(PROCESSORS);
            cmd.add("-cp");
            cmd.add(CP);
            cmd.add("-s");
            cmd.add(generatedJava.toString());
            cmd.add("-proc:only");

            System.out.println("+0+" + cmd);
            // System.out.println("-+++++++++++Java Compile code:\n\n\n " + str)

            compileAndMeasure(procNewJava, cmd);

            add_java_folder(generatedJava.getPath());
            System.out.println("🔋🔋🔋🔋🔋--Proceess DONE------------------------->");
        }
    }

    public void java() throws Exception {
        java("");
    }

    public void java(String addition) throws Exception {
        File builtJava = new File(build, "java");
        Utils.safty(builtJava, false, Utils.SAFTY_MODE_MAKE);

        String CP = (cp == null ? "" : "" + System.getProperty("path.separator") + cp);
        List<String> options = new ArrayList<>();

        options.add("-cp");
        options.add(androidJar + CP);
        options.add("-d");
        options.add(builtJava.getPath());

        compileAndMeasure(newJava, options);
    }
    /*
     * public void java() throws Exception {
     * java("");
     * }
     * 
     * public void java(String addition) throws Exception {
     * ensureBuild();
     * File builtJava = new File(build, "java");
     * UtilFile.dir(builtJava.getPath());
     * String CP = (cp == null ? "" : "" + System.getProperty("path.separator") +
     * cp);
     * java_proc();
     * List<String> options = new ArrayList<>();
     * options.add("-cp");
     * options.add(androidJar + CP);
     * options.add("-d");
     * options.add(builtJava.getPath());
     * 
     * qqqqaAhss compileAndMeasure(newJava, options);
     * }
     */

    public void dex(String input, String dst) throws Exception {
        // String builtJava = ensureDexing(new File(dst));
        Utils.safty(dst, false, Utils.SAFTY_MODE_MAKE);

        System.out.println("------>" + dst);
        System.out.println("------>" + input);
        System.out.println("------>" + new File(input).exists());

        toDexDebug(input, dst, androidJar.getPath());
        System.out.println("DEXed");
    }

    public void dexing() throws Exception {
        File classesFile = new File(build, "classes");
        Utils.safty(classesFile, false, Utils.SAFTY_MODE_MAKE);

        File builtJava = new File(build, "java");
        Utils.safty(builtJava, false, Utils.SAFTY_MODE_MAKE);

        dex(builtJava.getPath(), classesFile.getPath());

        mereging(classesFile);
    }

    private void mereging(File classesFile) throws Exception {
        File tmpFile = new File(pluginApkSigner.unsignedApkFile + "-tmp");
        ZipOutputStream tempZipOutStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
        byte[] buffer = new byte[8192];

        File[] baseDexes = classesFile.listFiles();
        int baseDexesSize = 0;
        for (File baseDex : baseDexes) {
            if (baseDex.getName().endsWith(".dex")) {
                baseDexesSize++;
                addToZip(baseDex, tempZipOutStream, buffer, baseDex.getName());
            }
        }

        if (baseDexesSize == 0) {
            throw new RuntimeException("E5: Error: -----base Dex Not found");
        }
        HashSet<String> namesToReplace = new HashSet<>();

        for (File baseDex : baseDexes) {
            if (baseDex.getName().endsWith(".dex")) {
                namesToReplace.add(baseDex.getName());
            }
        }
        int i = 0;
        for (File f : dexesOfJars.keySet()) {
            if (!f.exists()) {
                throw new RuntimeException("E6: Error: -:apsend dexOfJar " + dexesOfJars.get(f));
            }
            String countedName = UtilFile.getNameNoExtension(f) + (i + 1 + baseDexesSize) + UtilFile.getExtension(f);
            namesToReplace.add(countedName);
            addToZip(f, tempZipOutStream, buffer, countedName);
            i++;
        }

        ZipInputStream zipInputUnSigned = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(pluginApkSigner.unsignedApkFile)));
        ZipEntry entry;
        while ((entry = zipInputUnSigned.getNextEntry()) != null) {
            String name = entry.getName();
            if (namesToReplace.contains(name)) {
                continue;
            }
            ZipEntry newEntry = new ZipEntry(name);
            newEntry.setMethod(entry.getMethod());
            newEntry.setTime(entry.getTime());
            newEntry.setExtra(entry.getExtra());
            newEntry.setComment(entry.getComment());

            // 1. Read all bytes first
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int len;
            CRC32 crc = new CRC32();
            while ((len = zipInputUnSigned.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
                crc.update(buffer, 0, len); // calculate crc while reading
            }
            byte[] data = baos.toByteArray();

            // 2. If STORED, we MUST set size + crc before putNextEntry
            if (entry.getMethod() == ZipEntry.STORED) {
                newEntry.setSize(data.length);
                newEntry.setCompressedSize(data.length); // STORED = same size
                newEntry.setCrc(crc.getValue());
            }

            tempZipOutStream.putNextEntry(newEntry);
            tempZipOutStream.write(data); // write from memory
            tempZipOutStream.closeEntry();
            zipInputUnSigned.closeEntry();
        }
        System.out.println("ADDING-JNIS:");
        for (String jni : jnis) {
            System.out.println("ADD jni:🔋🔋" + getFileName(jni));
            jni_walker(new File(jni), jni, true, tempZipOutStream);
        }
        tempZipOutStream.close();
        zipInputUnSigned.close();
        pluginApkSigner.unsignedApkFile.delete();
        tmpFile.renameTo(pluginApkSigner.unsignedApkFile);

        String signed = pluginApkSigner.sign();
        run(adb.toString(), "push", signed);
        run(adb.toString(), "install", signed);
        run(adb.toString(), "shell", "monkey", "-p", pkgName, "-c", "android.intent.category.LAUNCHER", "1");
    }

    File build;

    public void build(String build) {
        this.build = new File(build);
        Utils.safty(build, false, Utils.SAFTY_MODE_MAKE);
    }

    public void jni_walker(File jni, String header, boolean first, ZipOutputStream tempZipOutStream)
            throws Exception {
        byte[] buffer = new byte[8192];

        if (first && !jni.exists()) {
            System.out.println("Error: -:APSENT jni folder " + jni);
            return;
        }

        for (File t : jni.listFiles()) {
            if (t.isDirectory()) {
                jni_walker(t, header, false, tempZipOutStream);
            } else {
                addToZip(t, tempZipOutStream, buffer, t.getPath().replaceAll(header, "lib"));
            }
        }
    }

    //
    public void addToZip(File file, ZipOutputStream tempZipOutStream, byte[] buffer, String nameInside)
            throws Exception {
        FileInputStream fis = new FileInputStream(file);
        // name inside zip. use only filename, not full path
        ZipEntry entry = new ZipEntry(nameInside);
        tempZipOutStream.putNextEntry(entry);

        int len;
        while ((len = fis.read(buffer)) > 0) {
            tempZipOutStream.write(buffer, 0, len);
        }

        tempZipOutStream.closeEntry();
        fis.close();
    }

    public void aar_model_folder(String modelFoldersPath) throws Exception {
        File modelFolders = new File(modelFoldersPath);
        Utils.safty(modelFolders, false);
        for (File model : modelFolders.listFiles()) {
            aar_model(model.getPath());
        }
    }

    public void aar_model(String aar_group) throws Exception {
        File aarGroupFolder = new File(aar_group);
        Utils.safty(aarGroupFolder, false);
        if (aarGroupFolder.isFile()) {
            if (aarGroupFolder.getName().endsWith(".aar")) {
                aar(aarGroupFolder.getPath());
            } else if (aarGroupFolder.getName().endsWith(".jar")) {
                add_jar(aarGroupFolder.getPath());
            }
            return;
        }

        add_jar_folder(aar_group);
        // System.out.println("AAR++++++APK:" + aar_group);
        // System.out.println("Aar: " + flat.getPath());
        // File aar_build = new File(aarParent + "/" + UtilFile.getNameNoExtension(aar)
        // + "_build");

        for (File aar : aarGroupFolder.listFiles()) {
            if (aar.getName().endsWith(".aar")) {
                aar(aar.getPath());
            } else if (aar.getName().endsWith(".jar")) {
                add_jar(aar.getPath());
            }
        }
    }

    private void aar(String aarPath) throws Exception {
        debuggigPrint("aar:" + aarPath);
        Utils.safty(aarPath, true);

        File aar = new File(aarPath);

        File unzipFolder = new File(aar.getParent(), UtilFile.getNameNoExtension(aar));
        if (!unzipFolder.exists()) {
            UtilFile.dir(unzipFolder.getPath());
            try {
                unzip(aar, unzipFolder);
            } catch (Exception e) {
                System.out.println("Unable to unzip aar🚫🚫🚫:{" + aar.getName() + "}" + e);
                return;
            }
            System.out.println("aar unzipped🔋🔋 {" + aar.getName() + "}");
        } else {
            System.out.println("🔋🔋Using cached🔋🔋 unzipped folder{" + aar.getName() + "}");
        }

        add_jar_folder(unzipFolder.getPath());
        File aarRes = new File(unzipFolder, "res");
        if (aarRes.exists()) {
            add_compile_res(aarRes);
        }
        File jni = new File(unzipFolder, "jni");
        add_jni(jni);
    }

    private void debuggigPrint(String string) {

    }

    public static void unzip(File zipFile, File destDir) throws Exception {
        destDir.mkdirs();
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                File newFile = new File(destDir, zipEntry.getName());

                // Security check - prevent Zip Slip attack
                String destDirPath = destDir.getPath();
                String destFilePath = newFile.getPath();
                if (!destFilePath.startsWith(destDirPath + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // create parent dirs
                    new File(newFile.getParent()).mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    private String getPackageFromAarManifest(File unzipFolder) {
        try {
            File manifest = new File(unzipFolder, "AndroidManifest.xml");
            if (!manifest.exists())
                return null;
            // Manifest is plain XML in AAR (not binary)
            String xml = new String(java.nio.file.Files.readAllBytes(manifest.toPath()));
            // find package="..."
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("package=\"([^\"]+)\"").matcher(xml);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            debuggigPrint("Manifest parse fail: " + e);
        }
        return null;
    }

    public void add_jni(File jni) {
        if (jni.exists()) {
            jnis.add(jni.getPath());
        } else {
            // System.out.println("Warning: Aar {" + jni + "} has no jni..");
        }
    }

    public static String getFileName(String path) {
        int i = path.lastIndexOf('/');
        if (i == -1)
            i = path.lastIndexOf('\\');
        return i == -1 ? path : path.substring(i + 1);
    }

    static void toDexDebug(String input, String out, String android) throws Exception {
        D8Command.Builder builder = D8Command.builder()
                .setMode(CompilationMode.DEBUG);

        toDex(input, out, android, builder);
    }

    static void toDexRelease(String input, String out, String android) throws Exception {
        D8Command.Builder builder = D8Command.builder()
                .setMode(CompilationMode.RELEASE);

        toDex(input, out, android, builder);
    }

    static void toDex(String input, String out, String android, D8Command.Builder builder)
            throws Exception {
        builder.addLibraryFiles(Paths.get(android))
                .setOutput(Paths.get(out), OutputMode.DexIndexed)
                .setMinApiLevel(21)
                .setDisableDesugaring(false);
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new RuntimeException("!!!!!!!!!!!!!!!!!!!!!!!!!!!!@#$" + inputFile);
        }
        if (inputFile.isFile())
            if (inputFile.getName().endsWith(".jar")) {
                builder.addProgramFiles(inputFile.toPath());
            } else {
                System.out.println("Filad D8: not jar file or not found." + inputFile);
                return;
            }
        else {
            addClassFiles(inputFile, builder);
        }
        try {
            D8.run(builder.build());
            System.out.println("D8 Successed:(OK):" + inputFile.getName());
        } catch (Exception e) {
            System.out.println("Filad D8\n----------------------D8-Error---------------------------\n " + e);
        }
    }

    private static void addClassFiles(File dir, D8Command.Builder builder) throws Exception {
        File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                addClassFiles(file, builder);
            } else if (file.getName().endsWith(".class")) {
                builder.addProgramFiles(file.toPath());
            }
        }
    }

    private static void compileAndMeasure(List<String> files, List<String> options)
            throws IOException {
        if (files.size() == 0) {
            return;
        }
        if (compiler == null) {
            System.out.println(NO_COMPILER_ERROR);
            return;
        }

        long startTime = System.nanoTime();
        // System.out.println("--Options--: " + options);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(files);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
                compilationUnits);
        boolean success = task.call();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        if (success) {
            System.out.println("OK(" + files.size() + ")file in " + durationMs + "ms");
        } else {
            System.out.println("*** *** *** *** *** ***Fail in " + durationMs + "ms  *** *** *** *** ***");
            System.out.println("         -----Errors-----         ");
            for (javax.tools.Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                String fileName = d.getSource() == null ? "Unknown" : new File(d.getSource().getName()).getName();
                System.out.printf("%s:line: %d  ----  -> %s  %n%n%s%n%n%n%n%n%n", d.getKind(), d.getLineNumber(),
                        fileName.replaceAll("\\.java\\s*$", ""), d.getMessage(null));
            }
        }
    }

    public void run(String... args) throws Exception {
        run(null, args);
    }

    public void run(File dir, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(dir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
        reader.close();
        p.waitFor();
    }
}