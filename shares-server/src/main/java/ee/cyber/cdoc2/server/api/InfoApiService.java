package ee.cyber.cdoc2.server.api;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ee.cyber.cdoc2.server.generated.api.InfoApiDelegate;


@Service
@RequiredArgsConstructor
public class InfoApiService implements InfoApiDelegate {

    private final List<InfoContributor> infoContributors;

    @Override
    public ResponseEntity<Map<String, Object>> getInfo() {
        Info.Builder builder = new Info.Builder();
        infoContributors.forEach(c -> c.contribute(builder));
        return ResponseEntity.ok(builder.build().getDetails());
    }

}
