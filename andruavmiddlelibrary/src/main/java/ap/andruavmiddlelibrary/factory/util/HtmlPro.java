package ap.andruavmiddlelibrary.factory.util;

/**
 * Created by M.Hefny on 10-Oct-14.
 */
public class HtmlPro {


    /**
     *
     * @param Color in the form #HEX  or name
     * @param Text
     * @param isNewLine
     * @param isBold
     * @return
     */
    public static String AddLine (final String Color, final String Text,final boolean isNewLine, final boolean isBold)
    {
        String res="";

        if (isNewLine) res = "<br>";
        res += "<font color='" + Color + "'>";

        if (isBold) res += "<b>";
        res += Text;
        if (isBold) res += "</b>";
        res += "</font>";
        return res;
    }




}
