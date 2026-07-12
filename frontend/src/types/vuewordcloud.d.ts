declare module 'vuewordcloud' {
	import type { DefineComponent } from 'vue';

	export interface VueWordCloudWordOptions {
		text: string;
		weight?: number;
		rotation?: number;
		rotationUnit?: 'turn' | 'deg' | 'rad';
		fontFamily?: string;
		fontStyle?: string;
		fontVariant?: string;
		fontWeight?: string;
		color?: string;
	}

	export type VueWordCloudWord = string | Array<string | number> | VueWordCloudWordOptions;

	export type VueWordCloudResolver<T> = (
		word: VueWordCloudWord,
		index: number,
		words: VueWordCloudWord[],
	) => T;

	export type VueWordCloudResolvable<T> = T | VueWordCloudResolver<T>;

	const VueWordCloud: DefineComponent<{
		animationDuration?: number;
		animationEasing?: string;
		animationOverlap?: number;
		color?: VueWordCloudResolvable<string>;
		fontFamily?: VueWordCloudResolvable<string>;
		fontSizeRatio?: number;
		fontStyle?: VueWordCloudResolvable<string>;
		fontVariant?: VueWordCloudResolvable<string>;
		fontWeight?: VueWordCloudResolvable<string>;
		rotation?: VueWordCloudResolvable<number>;
		rotationUnit?: VueWordCloudResolvable<'turn' | 'deg' | 'rad'>;
		spacing?: number;
		text?: VueWordCloudResolvable<string>;
		weight?: VueWordCloudResolvable<number>;
		words?: VueWordCloudWord[];
	}>;

	export default VueWordCloud;
}
