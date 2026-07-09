package RMC_Booking_Engine.rmc.util;

public final class BrandingColorUtil {

    private BrandingColorUtil() {}

    public static String contrastingForeground(String hexColor) {
        int[] rgb = hexToRgb(hexColor);
        double luminance = relativeLuminance(rgb);
        return luminance > 0.55 ? "#1a1a1a" : "#fafafa";
    }

    private static int[] hexToRgb(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        int value = Integer.parseInt(normalized, 16);
        return new int[] {(value >> 16) & 0xff, (value >> 8) & 0xff, value & 0xff};
    }

    private static double relativeLuminance(int[] rgb) {
        double r = channel(rgb[0]);
        double g = channel(rgb[1]);
        double b = channel(rgb[2]);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int value) {
        double normalized = value / 255.0;
        return normalized <= 0.03928
                ? normalized / 12.92
                : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
