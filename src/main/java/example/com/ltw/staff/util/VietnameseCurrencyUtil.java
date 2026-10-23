package example.com.ltw.staff.util;

import java.math.BigDecimal;

public class VietnameseCurrencyUtil {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };

    private static final String[] UNITS = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"
    };

    public static String convertToWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "Không đồng chẵn.";
        }

        long number = amount.longValue();
        if (number < 0) {
            return "Âm " + convertToWords(BigDecimal.valueOf(Math.abs(number)));
        }

        StringBuilder result = new StringBuilder();
        int unitIndex = 0;

        while (number > 0) {
            int group = (int) (number % 1000);
            if (group > 0) {
                String groupText = readThreeDigits(group, number >= 1000);
                String unit = UNITS[unitIndex];
                if (!unit.isEmpty()) {
                    groupText += " " + unit;
                }
                result.insert(0, groupText + " ");
            }
            number /= 1000;
            unitIndex++;
        }

        String res = result.toString().trim().replaceAll("\\s+", " ");
        if (!res.isEmpty()) {
            res = Character.toUpperCase(res.charAt(0)) + res.substring(1) + " đồng chẵn.";
        }
        return res;
    }

    private static String readThreeDigits(int number, boolean full) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int units = number % 10;
        StringBuilder sb = new StringBuilder();

        if (hundreds > 0 || full) {
            sb.append(DIGITS[hundreds]).append(" trăm ");
        }

        if (tens > 1) {
            sb.append(DIGITS[tens]).append(" mươi ");
            if (units == 1) {
                sb.append("mốt ");
            } else if (units == 5) {
                sb.append("lăm ");
            } else if (units > 0) {
                sb.append(DIGITS[units]).append(" ");
            }
        } else if (tens == 1) {
            sb.append("mười ");
            if (units == 5) {
                sb.append("lăm ");
            } else if (units > 0) {
                sb.append(DIGITS[units]).append(" ");
            }
        } else {
            if (units > 0) {
                if (hundreds > 0 || full) {
                    sb.append("lẻ ");
                }
                sb.append(DIGITS[units]).append(" ");
            }
        }

        return sb.toString().trim();
    }
}
