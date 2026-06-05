import IconSpeed from '~icons/material-symbols/speed-outline';
import IconUser from '~icons/material-symbols/person-outline';
import IconDocument from '~icons/material-symbols/article-outline';
// import IconCategory from '~icons/material-symbols/category-outline';
import IconCategory from '~icons/material-symbols/folder-open-outline';
import IconTag from '~icons/mdi/tag-outline';
// import IconSolarUser from '~icons/solar/user-outline';
// import IconSolarDocument from '~icons/solar/document-outline';
import IconView from '~icons/lucide/eye';
import IconComment from '~icons/lucide/message-square-more';
import IconLike from '~icons/lucide/thumbs-up';
import IconFavorite from '~icons/lucide/star';

const iconMap = {
	dashboard: IconSpeed,
	user: IconUser,
	article: IconDocument,
	category: IconCategory,
	tag: IconTag,
	view: IconView,
	comment: IconComment,
	like: IconLike,
	favorite: IconFavorite,
};

type AdminMenuIcon = keyof typeof iconMap;

export default function getRouteIcon(icon: unknown) {
	if (typeof icon === 'string' && icon in iconMap) {
		return iconMap[icon as AdminMenuIcon];
	}

	return undefined;
}
