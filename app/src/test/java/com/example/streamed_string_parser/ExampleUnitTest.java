package com.example.streamed_string_parser;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    final String mURL = "https://gist.githubusercontent.com/numdouglas/271e070a7a26073774675ae514467499/raw/e7f7204e52844c0a3644a1259ae9ccad77e6e9bc/test_strings.txt";

    @Test
    public void regex_playground() {

        String mask = "*";
        String text = "people should not govern\npeople should fly";
        text = runUrlFetch();
        System.out.println(text);

        CharSequence[] text_batch = text.split("\n");

        String new_mask = mask.replaceAll("\\*+", ".*").replaceAll("\\?", ".{1}");

        System.out.println("Final mask used " + new_mask);
        Pattern pattern = Pattern.compile("(?im)^" + new_mask + "$");

        for (CharSequence line : text_batch) {
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                System.out.println(matcher.group(0));
            }
        }
    }

    String runUrlFetch() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().
                url(mURL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            BufferedSource bufferedSource = response.body().source();

            Scanner scanner = new Scanner(bufferedSource);
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }

            bufferedSource.close();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Test
    public void testUrlAsStreamFetch() throws IOException {
        InputStream is = new URL(mURL).openStream();
        Scanner sc = new Scanner(is, "cp1252");
        while (sc.hasNextLine()) {
            System.out.println(sc.nextLine());
        }
        is.close();
        sc.close();
    }

    @Test
    public void testTrimOp() {

        SimpleDateFormat log_df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        System.out.println(log_df.format(timestamp));

    }

    @Test
    public void testTextBlockScan() {
        String textBlock = "hello i look like a very big onion.\nBut I wont make you cry.";

        Scanner scanner = new Scanner(textBlock);

        scanner.useDelimiter(Pattern.compile(""));

        String newlinews = "\n";
        StringBuilder builder = new StringBuilder();

        while (scanner.hasNext()) {
//            if(scanner.next().equals("\r")){System.out.println("Sasasas");}
            String character = scanner.next();

            if (character.equals(newlinews)) {
                System.out.println(builder);
                builder.delete(0, builder.length());
            } else {
                builder.append(character);
            }
        }
        System.out.println(builder);

/*        while (scanner.hasNextLine()) {
            System.out.println(scanner.nextLine());
        }*/
        scanner.close();
    }
}