export interface CommentSticker {
	token: string;
	name: string;
	url: string;
}

export interface CommentStickerPack {
	id: string;
	name: string;
	iconUrl: string;
	gridMinWidthRem?: number;
	items: CommentSticker[];
}
