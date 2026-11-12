package com.ccsanjuu.blog.modules.comment.service.impl;

import com.ccsanjuu.blog.common.validation.StickerContentLengthValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommentStickerServiceImplTest {

    private static CommentStickerServiceImpl service;

    @BeforeAll
    static void loadCatalog() throws Exception {
        service = new CommentStickerServiceImpl(new ObjectMapper());
    }

    @Test
    void shouldPreservePackOrderFormatsAndOriginalNames() {
        assertEquals(5, service.getPacks().size());
        var bilibili = service.getPacks().stream().filter(pack -> pack.id().equals("bilibili")).findFirst().orElseThrow();
        assertEquals("Bili 小电视", bilibili.name());
        assertEquals("/stickers/bilibili/bb_cute.png", bilibili.iconUrl());
        assertEquals("[sticker:bilibili:doge]", bilibili.items().getFirst().token());
        assertNull(bilibili.gridMinWidthRem());
        assertEquals(4.5, service.getPacks().stream().filter(pack -> pack.id().equals("alus"))
                .findFirst().orElseThrow().gridMinWidthRem());
        assertTrue(service.getPacks().stream().anyMatch(pack -> pack.iconUrl().endsWith(".gif")));
        assertTrue(service.getPacks().stream().anyMatch(pack -> pack.iconUrl().endsWith(".webp")));
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
