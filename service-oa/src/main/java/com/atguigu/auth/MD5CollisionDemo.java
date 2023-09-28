package com.atguigu.auth;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MD5CollisionDemo {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        // 输入1
        String input1 = "Hello, MD5!";
        // 输入2
        String input2 = "Goodbye, MD5!";

        // 计算输入1的MD5散列值
        byte[] hash1 = calculateMD5(input1);
        System.out.println("MD5 hash of input1: " + byteArrayToHexString(hash1));

        // 计算输入2的MD5散列值
        byte[] hash2 = calculateMD5(input2);
        System.out.println("MD5 hash of input2: " + byteArrayToHexString(hash2));

        // 检查是否存在碰撞
        boolean collision = Arrays.equals(hash1, hash2);
        System.out.println("Collision detected: " + collision);
    }

    // 计算MD5散列值
    private static byte[] calculateMD5(String input) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        return md5.digest(input.getBytes());
    }

    // 将字节数组转换为16进制字符串
    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
