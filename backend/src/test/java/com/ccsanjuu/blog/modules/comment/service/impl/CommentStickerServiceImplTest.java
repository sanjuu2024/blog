package com.ccsanjuu.blog.modules.comment.service.impl;

import com.ccsanjuu.blog.common.validation.StickerContentLengthValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommentStickerServiceImplTest {

    private static CommentStickerServiceImpl service;

    @BeforeAll
    static void loadCatalog() throws Exception {
        service = new CommentStickerServiceImpl(new ObjectMapper());
    }

    @Test
    void shouldPreservePackOrderFormatsAndOriginalNames() {
        assertEquals(List.of("2233", "alus", "azuki", "bangumi", "bilibili", "bmoji", "kanae", "natsume",
                        "qq", "qyn", "rimuru", "saikikusuo", "sharkitty", "suzume", "wechat"),
                service.getPacks().stream().map(pack -> pack.id()).toList());
        assertEquals("哔哩哔哩 2233娘", service.getPacks().getFirst().name());
        assertEquals(2.5, service.getPacks().getFirst().minHeightRem());
        assertEquals("暹罗猫小豆泥", service.getPacks().get(2).name());
        assertEquals(3, service.getPacks().get(2).minHeightRem());
        var bilibili = service.getPacks().stream().filter(pack -> pack.id().equals("bilibili")).findFirst().orElseThrow();
        assertEquals("哔哩哔哩小电视", bilibili.name());
        assertTrue(bilibili.iconUrl().matches("/stickers/bilibili/btv_cute\\.png\\?v=[0-9a-f]{16}"));
        assertEquals("[sticker:bilibili:doge]", bilibili.items().getFirst().token());
        Map<String, Double> heights = Map.ofEntries(
                Map.entry("2233", 2.5), Map.entry("alus", 2.0), Map.entry("azuki", 3.0),
                Map.entry("bangumi", 3.0), Map.entry("kanae", 3.0), Map.entry("natsume", 4.0),
                Map.entry("qyn", 3.0), Map.entry("rimuru", 3.0), Map.entry("saikikusuo", 3.0),
                Map.entry("sharkitty", 3.0), Map.entry("suzume", 3.0));
        for (var pack : service.getPacks()) {
            assertEquals(heights.get(pack.id()), pack.minHeightRem(), pack.id());
        }
        var bangumi = service.getPacks().stream().filter(pack -> pack.id().equals("bangumi")).findFirst().orElseThrow();
        assertEquals("Bangumi 娘", bangumi.name());
        assertTrue(bangumi.items().stream().allMatch(item -> item.url().startsWith("/stickers/bangumi/bangumi_")));
        assertEquals("哔哩哔哩表情", service.getPacks().stream().filter(pack -> pack.id().equals("bmoji"))
                .findFirst().orElseThrow().name());
        assertEquals(791, service.getPacks().stream().mapToInt(pack -> pack.items().size()).sum());
        assertTrue(service.getPacks().stream().anyMatch(pack -> pack.iconUrl().contains(".gif?v=")));
        assertTrue(service.getPacks().stream().anyMatch(pack -> pack.iconUrl().contains(".webp?v=")));
        for (var pack : service.getPacks()) {
            assertTrue(pack.iconUrl().matches("/stickers/[^?]+\\?v=[0-9a-f]{16}"), pack.id());
            assertTrue(pack.items().stream().allMatch(item -> item.url()
                    .matches("/stickers/[^?]+\\?v=[0-9a-f]{16}")), pack.id());
        }
    }

    @Test
    void shouldRecognize2233AndAzukiTokensAndKeepTheirTextFallback() {
        assertEquals(15, service.getPacks().getFirst().items().size());
        assertEquals(35, service.getPacks().get(2).items().size());
        String content = "[sticker:2233:大笑][sticker:azuki:抱大腿]";
        assertEquals(2, service.countLength(content));
        assertEquals(2, service.countStickers(content));
        assertEquals("[大笑][抱大腿]", service.toPlainText(content));
    }

    @Test
    void shouldRecognizeTokensFromTheFourNewPacks() {
        String content = "[sticker:natsume:归还名字][sticker:rimuru:比心]"
                + "[sticker:saikikusuo:发动超能力][sticker:suzume:递猫]";
        assertEquals(4, service.countLength(content));
        assertEquals(4, service.countStickers(content));
        assertEquals("[归还名字][比心][发动超能力][递猫]", service.toPlainText(content));
    }

    @Test
    void shouldRecognizeSharkittyTokensAndKeepTheirTextFallback() {
        String token = "[sticker:sharkitty:不想看]";
        assertEquals(1, service.countLength(token));
        assertEquals(1, service.countStickers(token));
        assertEquals("[不想看]", service.toPlainText(token));
    }

    @Test
    void shouldCountKnownStickersAsOneAndUnknownOrMalformedTokensAsText() {
        String token = "[sticker:bilibili:doge]";
        assertEquals(1000, service.countLength("字".repeat(999) + token));
        assertEquals(1000, service.countLength(token.repeat(1000)));
        for (String unknown : new String[]{"[sticker:missing:doge]", "[sticker:bilibili:missing]", "[sticker:bilibili:doge"}) {
            assertEquals(unknown.length(), service.countLength(unknown));
        }
        assertEquals(2, service.countLength("😀"));
    }

    @Test
    void validatorShouldUseDisplayLengthAfterTrimming() {
        var validator = new StickerContentLengthValidator(service);
        String token = "[sticker:bilibili:doge]";
        assertTrue(validator.isValid("  " + "字".repeat(980) + token.repeat(20) + "  ", null));
        assertFalse(validator.isValid(token.repeat(21), null));
        assertFalse(validator.isValid("字".repeat(1000) + "[sticker:bilibili:doge]", null));
        assertTrue(validator.isValid("[sticker:missing:doge]".repeat(21), null));
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void shouldCountRepeatedAndMixedStickersButIgnoreUnknownTokens() {
        assertEquals(20, service.countStickers("[sticker:bilibili:doge]".repeat(19) + "[sticker:wechat:微笑]"));
        assertEquals(21, service.countStickers("[sticker:bilibili:doge]".repeat(21)));
        assertEquals(0, service.countStickers("[sticker:missing:doge][sticker:bilibili:missing][sticker:bilibili:doge"));
    }

    @Test
    void shouldProduceTextWithoutInterpretingHtmlOrUnknownTokens() {
        assertEquals("<script>[doge][微笑][sticker:missing:doge]</script>", service.toPlainText(
                "<script>[sticker:bilibili:doge][sticker:wechat:微笑][sticker:missing:doge]</script>"));
        assertNull(service.toPlainText(null));
    }
}
