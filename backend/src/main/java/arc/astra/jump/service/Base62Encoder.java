package arc.astra.jump.service;

public class Base62Encoder {
    private final static String BASE62_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String encode(long number) {
        StringBuilder sb = new StringBuilder();
        if (number == 0) return "0";
        while (number > 0) {
            sb.append(BASE62_CHARACTERS.charAt((int) (number % 62)));
            number /= 62;
        }
        return sb.reverse().toString();

    }
}
