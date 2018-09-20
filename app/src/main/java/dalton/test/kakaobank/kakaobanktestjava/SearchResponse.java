package dalton.test.kakaobank.kakaobanktestjava;

import java.util.List;

public class SearchResponse {
    List<SearchEntry> documents;
    Meta meta;

    class Meta {
        boolean is_end;
        long total_count;
        long pageable_count;
    }
}