export interface Sticker {
	token: string;
	name: string;
	url: string;
	minHeightRem?: number;
}

export interface StickerPack {
	id: string;
	name: string;
	iconUrl: string;
	minHeightRem?: number;
	items: Sticker[];
}
