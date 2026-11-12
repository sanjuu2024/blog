package com.ccsanjuu.blog.modules.comment.service.impl;

import com.ccsanjuu.blog.modules.comment.model.vo.CommentStickerPackVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentStickerVO;
import com.ccsanjuu.blog.modules.comment.service.CommentStickerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CommentStickerServiceImpl implements CommentStickerService {

    private static final Pattern TOKEN = Pattern.compile("\\[sticker:[a-z0-9_-]+:[^\\[\\]:\\r\\n]+]");

    private final List<CommentStickerPackVO> packs;
    private final Map<String, CommentStickerVO> stickers;

    /**
     * 在启动时读取生成目录，目录缺失或重复标记时拒绝启动。
     *
     * @param objectMapper 项目统一 JSON 转换器
     * @throws IOException 目录无法读取或格式无效
     */
    public CommentStickerServiceImpl(ObjectMapper objectMapper) throws IOException {
        try (InputStream stream = new ClassPathResource("comment-stickers/catalog.json").getInputStream()) {
            packs = List.copyOf(objectMapper.readValue(stream, new TypeReference<List<CommentStickerPackVO>>() {}));
        }
        stickers = packs.stream().flatMap(pack -> pack.items().stream())
                .collect(Collectors.toUnmodifiableMap(CommentStickerVO::token, Function.identity()));
    }

    @Override
    public List<CommentStickerPackVO> getPacks() {
        return packs;
    }

    @Override
    public int countLength(String content) {
        int length = content.length();
        Matcher matcher = TOKEN.matcher(content);
        while (matcher.find()) {
            if (stickers.containsKey(matcher.group())) {
                length -= matcher.end() - matcher.start() - 1;
            }
        }
        return length;
    }

    @Override
    public int countStickers(String content) {
        int count = 0;
        Matcher matcher = TOKEN.matcher(content);
        while (matcher.find()) {
            if (stickers.containsKey(matcher.group())) count++;
        }
        return count;
    }

    @Override
    public String toPlainText(String content) {
        if (content == null) return null;
        return TOKEN.matcher(content).replaceAll(match -> {
            CommentStickerVO sticker = stickers.get(match.group());
            return Matcher.quoteReplacement(sticker == null ? match.group() : "[" + sticker.name() + "]");
        });
    }
}
