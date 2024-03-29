package com.example.catl.hulibarry.ulite;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;


import com.example.catl.hulibarry.hepler.ReflectHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

/**
 * Created by HSL on 2020/12/23.
 */

public class FileUtils {


    public static final String TAG = "FileUtils";

    private static final String android_os_FileUtils = "android.os.FileUtils";
    public static final CharSequence DATEFORMAT = "yyyyMMdd_kkmmss";
    public static String ILLEGAL_CHARS = '\\' + "/:?*<>|" + '"';
    private static final Random tempFileRandom = new Random();

    public static int setPermissions(String file, int mode, int uid, int gid) {
        Object value = ReflectHelper.invokeStaticMethod(android_os_FileUtils, "setPermissions", new Class<?>[]{
                String.class, int.class, int.class, int.class}, new Object[]{file, mode, uid, gid});
        int result = -1;
        if (value != null) {
            result = (Integer) value;
        }
        LogUtil.w(TAG, "setPermissions: file=" + file + ", mode=" + Integer.toOctalString(mode) + ", uid=" + uid
                + ", gid=" + gid + ", result=" + result);
        return result;
    }

    public static int getPermissions(String file, int[] outPermissions) {
        Object value = ReflectHelper.invokeStaticMethod(android_os_FileUtils, "getPermissions", new Class<?>[]{
                String.class, int[].class}, new Object[]{file, outPermissions});
        int result = -1;
        if (value != null) {
            result = (Integer) value;
        }
        LogUtil.w(TAG, "getPermissions: file = " + file + " result = " + Integer.toOctalString(result));
        return result;
    }

    public static synchronized File createTempFile(Context context, File directory, String prefix, String suffix)
            throws IOException {
        if (suffix == null) {
            suffix = ".tmp";
        }
        if (directory == null)
            directory = getDirectory(context, "tmp");
        File result;
        do {
            result = new File(directory, prefix + tempFileRandom.nextInt() + suffix);
        }
        while (result.exists());
        result.createNewFile();
        return result;
    }

    public static boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                result = saveToFile(in, destFile, false);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        LogUtil.v(TAG, "copyFile " + srcFile + " to " + destFile + " result = " + result);
        return result;
    }

    /*
     * 字符保存成文件
     * */
    public static boolean saveToFile(InputStream inputStream, File destFile, boolean append) {
        try {
            if (!destFile.exists()) {
                destFile.createNewFile();
            }
            OutputStream out = new FileOutputStream(destFile, append);
            try {
                byte[] buffer = new byte[4096];
                int readLength;
                while ((readLength = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, readLength);
                }
            } finally {
                out.close();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String makeNameInTime(long inTimeInMillis) {
        return DateFormat.format(DATEFORMAT, inTimeInMillis).toString();
    }

    public static String makeNameInCurrentTime() {
        return makeNameInTime(System.currentTimeMillis());
    }

    // 清除文件名中的非法字符
    public static String clearFileNameIllegalChars(String fileName) {
        if (fileName == null) {
            return null;
        }
        int length = fileName.length();
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < length; i++) {
            char c = fileName.charAt(i);
            if (ILLEGAL_CHARS.indexOf(c) != -1) {
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    private static long checksum(File file, Checksum csum) throws FileNotFoundException, IOException {
        CheckedInputStream cis = null;

        try {
            cis = new CheckedInputStream(new FileInputStream(file), csum);
            byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) {
                // Just read for checksum to get calculated.
            }
            return csum.getValue();
        } finally {
            if (cis != null) {
                try {
                    cis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Computes the checksum of a file using the CRC32 checksum routine.计算文件的校验和使用CRC32校验和例程。
     * The value of the checksum is returned.返回的校验值
     *
     * @param file the file to checksum, must not be null    校验文件不能为空
     * @return the checksum value or an exception is thrown.    校验和值或抛出异常
     */
    public static long checksumCrc32(File file) throws FileNotFoundException, IOException {
        CRC32 checkSummer = new CRC32();
        return checksum(file, checkSummer);
    }

    /**
     * Computes the checksum of a file using the ADLER32 checksum routine.  计算文件的校验和使用ADLER32校验和例程。
     * The value of the checksum is returned.   返回的校验值
     *
     * @param file the file to checksum, must not be null   校验文件不能为空
     * @return the checksum value or an exception is thrown.    校验和值或抛出异常
     */
    public static long checksumAdler32(File file) throws FileNotFoundException, IOException {
        Adler32 checkSummer = new Adler32();
        return checksum(file, checkSummer);
    }

    //删除文件
    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }

    //删除目录
    public static boolean deleteDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }
        for (File file : directory.listFiles()) {
            if (file != null) {
                if (file.isFile()) {
                    file.delete();
                } else {
                    deleteDirectory(file);
                }
            }
        }
        return directory.delete();
    }

    public static void deleteFiles(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        for (File file : directory.listFiles()) {
            if (file != null && file.isFile()) {
                file.delete();
            }
        }
    }

    public static void deleteFilesInChildren(File directory) {
        deleteFiles(directory);
        for (File file : directory.listFiles()) {
            if (file != null && file.isDirectory()) {
                deleteFilesInChildren(file);
            }
        }
    }

    /**
     * 判断文件是否存在
     */
    public static boolean checkFileExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
     * 检查目录
     */
    public static boolean checkDirectory(String path) {
        File file = new File(path);
        if (!file.exists())
            return file.mkdirs();
        return file.isDirectory();
    }

    /**
     * 判断外部存储器(SDCard)是否存在
     */
    public static boolean checkExternalStorageMounted() {
        String state = Environment.getExternalStorageState();
        LogUtil.d(TAG, "getExternalStorageState = " + state);
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    /**
     * 获取文件目录可用存储空间
     */
    public static long getStatFsAvailableSize(String path) {
        File file = new File(path);
        if (!file.isDirectory()) {
            path = file.getParent();
        }
        LogUtil.i(TAG, "getStatFsAvailableSize: path = " + path);
        StatFs statfs = new StatFs(path);
        long blockSize = statfs.getBlockSize();
        // int totalBlocks = statfs.getBlockCount();
        long availableBlocks = statfs.getAvailableBlocks();
        long availableSize = blockSize * availableBlocks;
        LogUtil.d(TAG, "getStatFsAvailableSize: " + availableSize);
        return availableSize;
    }

    /**
     * 获取文件目录总存储空间
     */
    public static long getStatFsTotalSize(String path) {
        File file = new File(path);
        if (!file.isDirectory()) {
            path = file.getParent();
        }
        LogUtil.i(TAG, "getStatFsTotalSize: path = " + path);
        StatFs statfs = new StatFs(path);
        long blockSize = statfs.getBlockSize();
        long totalBlocks = statfs.getBlockCount();
        // int availableBlocks = statfs.getAvailableBlocks();
        long totalSize = blockSize * totalBlocks;
        LogUtil.d(TAG, "getStatFsTotalSize: " + totalSize);
        return totalSize;
    }

    /**
     * 判断某文件目录是否有足够的存储空间
     */
    public static boolean hasEnoughAvailableSize(String path, long fileLength) {
        LogUtil.v(TAG, "hasEnoughAvailableSize: " + path + " " + fileLength);
        if (TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("path = " + path);
        }
        if (fileLength > 0) {
            if (fileLength < getStatFsAvailableSize(path)) {
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException("length = " + fileLength);
        }
    }

    /**
     * 获取并创建一个程序内部存储目录
     */
    public static File getInternalDirectory(Context context, String dirName, int mode) {
        if (TextUtils.isEmpty(dirName)) {
            throw new NullPointerException("the dirName is null!!!");
        }
        return context.getDir(dirName, mode);
    }

    public static File getInternalDirectory(Context context, String dirName) {
        return getInternalDirectory(context, dirName, Context.MODE_PRIVATE);
    }

    /**
     * 获取程序外部存储根目录
     */
    public static File getExternalRootDirectory(Context context) {
        File directory = new File(Environment.getExternalStorageDirectory(), context.getPackageName());
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    /**
     * 获取并创建一个程序外部存储目录
     */
    public static File getExternalDirectory(Context context, String dirName) {
        if (TextUtils.isEmpty(dirName)) {
            throw new NullPointerException("the dirName is null!!!");
        }
        File directory = new File(getExternalRootDirectory(context), dirName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    /**
     * 获取并创建一个程序目录，优先外部存储器
     */
    public static File getDirectory(Context context, String dirName, int mode) {
        if (checkExternalStorageMounted()) {
            return getExternalDirectory(context, dirName);
        } else {
            return context.getDir(dirName, mode);
        }
    }

    public static File getDirectory(Context context, String dirName) {
        return getDirectory(context, dirName, Context.MODE_PRIVATE);
    }

    /**
     * 获取并创建一个在程序目录下的文件，优先外部存储器
     */
    public static File getFile(Context context, String dirName, String fileName, int mode) {
        if (TextUtils.isEmpty(fileName)) {
            throw new NullPointerException("the fileName is null!!!");
        }
        File directory = getDirectory(context, dirName, mode);
        File file = new File(directory, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LogUtil.w(TAG, "createNewFile failed, " + file);
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File getFile(Context context, String dirName, String fileName) {
        return getFile(context, dirName, fileName, Context.MODE_PRIVATE);
    }

    public static class FileNameInfo {
        public String dirPath;
        /**
         * @deprecated Use {@link #fullName} instead.
         */
        public String name;
        public String fullName;
        public String prefix;
        public String suffix;
    }

    /*
     * 从一个url地址中获取文件全名
     */
    public static String getFileFullName(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        int index = url.lastIndexOf('/');
        // if (index == -1)
        // {
        // return url;
        // }
        String fileName = url.substring(index + 1);
        // fileName = clearFileNameIllegalChars(fileName);
        return fileName;
    }

    /**
     * @deprecated Use {@link #getFileFullName} instead.
     */
    public static String getFileName(String url) {
        return getFileFullName(url);
    }

    /**
     * 解析文件名
     *
     * @return FileName
     */
    public static FileNameInfo parseFileName(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        FileNameInfo fileName = new FileNameInfo();
        int index = url.lastIndexOf('/');
        fileName.dirPath = url.substring(0, index);
        fileName.fullName = fileName.name = url.substring(index + 1);
        index = fileName.fullName.lastIndexOf('.');
        if (index == -1) {
            fileName.prefix = fileName.fullName;
            fileName.suffix = null;
        } else {
            fileName.prefix = fileName.fullName.substring(0, index);
            fileName.suffix = fileName.fullName.substring(index);
        }
        return fileName;
    }

    //TXT文本的编码方式有UNICODES，UTF8，UTF16等等。 这里只针对UTF8编码的TXT文本作为例子。保存该txt文本到APP的目录xia。
    public static boolean saveText(Context context, String text) {

        if (text == null) return false;

        byte[] data = new byte[0];
        try {
            data = text.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (null == data || data.length == 0) {
            return false;
        }
        FileOutputStream fout = null;
        try {
            File file = context.getFilesDir();

            // make sure the path is valid and directories created for this file.
            File parentFile = file.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                return false;
            }


            fout = new FileOutputStream(file.toString() + "/request.txt");
            int size = data.length;
            //UTF8 TEXT HEAD
            byte[] heads = new byte[3];
            heads[0] = (byte) (-17);
            heads[1] = (byte) (-69);
            heads[2] = (byte) (-65);
            fout.write(heads, 0, heads.length);
            fout.write(data, 0, size);

            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)));
        } catch (IOException e) {
            // Ignore
            Log.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Log.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    public interface CopyRequestCallback {
        void onCopySuccess(String status);
    }

    /**
     * 复制本地数据库
     * */
    public static void copyDBToSDcrad(CopyRequestCallback callback) {
        String DATABASE_NAME = "lead_dhc_db.db";

        String oldPath = "data/data/com.lide.app/databases/" + DATABASE_NAME;
        String newPath = Environment.getExternalStorageDirectory() + File.separator + DateUtils.getStringDate("yyMMdd") + DATABASE_NAME;
        copyFile(oldPath, newPath, callback);
    }

    /**
     * 复制单个文件
     *
     * @param oldPath  String 原文件路径
     * @param newPath  String 复制后路径
     * @param callback
     * @return boolean
     */
    public static void copyFile(String oldPath, String newPath, CopyRequestCallback callback) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            File newfile = new File(newPath);
            if (!newfile.exists()) {
                newfile.createNewFile();
            }
            if (oldfile.exists()) { // 文件存在时
                InputStream inStream = new FileInputStream(oldPath); // 读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; // 字节数 文件大小
                    fs.write(buffer, 0, byteread);
                }
                callback.onCopySuccess(newPath);
                inStream.close();
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
            callback.onCopySuccess("复制出错");
        }
    }


    /**
     * 获取目录下的文件
     */
    public static List<String> getFilesAllName(String path) {
        if (checkDirectory(path)) {
            File file = new File(path);
            File[] files = file.listFiles();
            if (files == null) {
                Log.e("error", "空目录");
                return null;
            }
            List<String> s = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                s.add(files[i].getAbsolutePath());
            }
            return s;
        } else {
            return null;
        }
    }

    /**
     * 读取TXT文件
     */
    public static String loadFromSDFile(String fname) {
        String result = null;
        try {
            File f = new File(fname);
            int length = (int) f.length();
            byte[] buff = new byte[length];
            FileInputStream fin = new FileInputStream(f);
            fin.read(buff);
            fin.close();
            result = new String(buff, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
