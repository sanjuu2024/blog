export interface Sticker {
	token: string;
	name: string;
	url: string;
}

export interface StickerPack {
	id: string;
	name: string;
	iconUrl: string;
	gridMinWidthRem?: number;
	items: Sticker[];
}
