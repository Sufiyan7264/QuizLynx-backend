package com.projectJava.quizApp.utility;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Component
@Getter
public class OtpUtil {

    @Value("${otp.hmac.secret}")
    private String hmacSecret;
    @Value("${otp.length}")
    private int otpLength;
    @Value("${otp.expiration.minutes}")
    private int otpExpirationMinutes;

    public String generateOtp() throws NoSuchAlgorithmException {
        int max = (int) Math.pow(10,otpLength);
        SecureRandom secureRandom=new SecureRandom();
        int value = secureRandom.nextInt(max);
        return String.format("%0" + otpLength + "d", value);
    }

    public String hmacSha256hex(String input){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(hmacSecret.getBytes(),"HmacSHA256");
            mac.init(spec);
            byte[] raw = mac.doFinal(input.getBytes());
            return HexFormat.of().formatHex(raw);
        }
        catch (Exception e){
            throw new RuntimeException("Failed to calculate Hmac : "+e.getMessage());
        }
    }

    public Boolean constantTimeEquals(String aHex, String bHex){
        byte[] a = HexFormat.of().parseHex(aHex);
        byte[] b = HexFormat.of().parseHex(bHex);
        return MessageDigest.isEqual(a,b);
    }

    public LocalDateTime calculateExpiry() {
        return LocalDateTime.now().plusMinutes(otpExpirationMinutes);
    }
}
