package dalton.test.kakaobank.kakaobanktestjava;

import java.util.Date;

public class SearchEntry {
    public Date datetime;
    private String thumbnail;
    private String thumbnail_url;

    public String getThumbnail() {
        if (thumbnail_url != null) {
            return thumbnail_url;
        } else {
            return thumbnail;
        }
    }

    @Override public String toString() {
        return "\n" + datetime + "\t" + thumbnail + "\t" + thumbnail_url;
    }
}


/*

2018-09-15T00:27:24.000+09:00

 */