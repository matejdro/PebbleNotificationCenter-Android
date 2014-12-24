package timber.log;

import android.util.Log;
import com.matejdro.pebblenotificationcenter.util.LogWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logging for lazy people. */
// From https://github.com/JakeWharton/timber
public final class Timber {
    /** Log a debug message with optional format args. */
    public static void d(String message) {
        TREE_OF_SOULS.d(message);
    }

    /** Log a debug exception and a message with optional format args. */
    public static void d(Throwable t, String message) {
        TREE_OF_SOULS.d(t, message);
    }

    /** Log an info message with optional format args. */
    public static void i(String message) {
        TREE_OF_SOULS.i(message);
    }

    /** Log an info exception and a message with optional format args. */
    public static void i(Throwable t, String message) {
        TREE_OF_SOULS.i(t, message);
    }

    /** Log a warning message with optional format args. */
    public static void w(String message) {
        TREE_OF_SOULS.w(message);
    }

    /** Log a warning exception and a message with optional format args. */
    public static void w(Throwable t, String message) {
        TREE_OF_SOULS.w(t, message);
    }

    /** Log an error message with optional format args. */
    public static void e(String message) {
        TREE_OF_SOULS.e(message);
    }

    /** Log an error exception and a message with optional format args. */
    public static void e(Throwable t, String message) {
        TREE_OF_SOULS.e(t, message);
    }

    /** Set a one-time tag for use on the next logging call. */
    public static Tree tag(String tag) {
        TREE_OF_SOULS.tag(tag);

        return TREE_OF_SOULS;
    }

    private static final TaggedTree TREE_OF_SOULS = new DebugTree();

    private Timber() {
    }

    public interface Tree {
        /** Log a debug message with optional format args. */
        void d(String message);

        /** Log a debug exception and a message with optional format args. */
        void d(Throwable t, String message);

        /** Log an info message with optional format args. */
        void i(String message);

        /** Log an info exception and a message with optional format args. */
        void i(Throwable t, String message);

        /** Log a warning message with optional format args. */
        void w(String message);

        /** Log a warning exception and a message with optional format args. */
        void w(Throwable t, String message);

        /** Log an error message with optional format args. */
        void e(String message);

        /** Log an error exception and a message with optional format args. */
        void e(Throwable t, String message);
    }

    public interface TaggedTree extends Tree {
        /** Set a one-time tag for use on the next logging call. */
        void tag(String tag);
    }

    /** A {@link Tree} for debug builds. Automatically infers the tag from the calling class. */
    public static class DebugTree implements TaggedTree {
        private static final Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");
        private String nextTag;

        private String createTag() {
            String tag = nextTag;
            if (tag != null) {
                nextTag = null;
                return tag;
            }

            tag = new Throwable().getStackTrace()[4].getClassName();
            Matcher m = ANONYMOUS_CLASS.matcher(tag);
            if (m.find()) {
                tag = m.replaceAll("");
            }
            return tag.substring(tag.lastIndexOf('.') + 1);
        }

        @Override public void d(String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.d("PebbleNotificationCenter", text);
            LogWriter.write("D ".concat(text));
        }

        @Override public void d(Throwable t, String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.d("PebbleNotificationCenter", text, t);
            LogWriter.write("D ".concat(text));
        }

        @Override public void i(String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.i("PebbleNotificationCenter", text);
            LogWriter.write("I ".concat(text));
        }

        @Override public void i(Throwable t, String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.i("PebbleNotificationCenter", text, t);
            LogWriter.write("I ".concat(text));
        }

        @Override public void w(String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.w("PebbleNotificationCenter", text);
            LogWriter.write("W ".concat(text));
        }

        @Override public void w(Throwable t, String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.w("PebbleNotificationCenter", text, t);
            LogWriter.write("W ".concat(text));
        }

        @Override public void e(String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.e("PebbleNotificationCenter", text);
            LogWriter.write("E ".concat(text));
        }

        @Override public void e(Throwable t, String message) {
            String text = "[".concat(createTag()).concat("] ").concat(message);
            Log.e("PebbleNotificationCenter", text, t);
            LogWriter.write("E ".concat(text));
        }

        @Override public void tag(String tag) {
            nextTag = tag;
        }
    }
}
