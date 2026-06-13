package arc.astra.shrtnr.service;

public class Base62Encoder {
    private final static String BASE62_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String encode(int number) {
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            sb.append(BASE62_CHARACTERS.charAt((number % 62)));
            number /= 62;
        }
        return sb.reverse().toString();

    }
}
