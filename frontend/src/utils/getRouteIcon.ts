// import IconSpeed from '~icons/material-symbols/speed-outline';
// import IconUser from '~icons/material-symbols/person-outline';
// import IconDocument from '~icons/material-symbols/article-outline';
// import IconCategory from '~icons/material-symbols/folder-open-outline';
// import IconTag from '~icons/mdi/tag-outline';
import IconSpeed from '~icons/lucide/gauge';
import IconUser from '~icons/lucide/circle-user';
import IconDocument from '~icons/lucide/file-text';
import IconCategory from '~icons/lucide/folder-open';
import IconTag from '~icons/lucide/tag';
import IconView from '~icons/lucide/eye';
// import IconComment from '~icons/lucide/message-square-more';
import IconComment from '~icons/lucide/message-circle-more';
import IconLike from '~icons/lucide/thumbs-up';
import IconFavorite from '~icons/lucide/star';
import IconMessage from '~icons/lucide/message-square-text';
import IconAudit from '~icons/lucide/scroll-text';

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
	message: IconMessage,
	audit: IconAudit,
};

type AdminMenuIcon = keyof typeof iconMap;

export default function getRouteIcon(icon: unknown) {
	if (typeof icon === 'string' && icon in iconMap) {
		return iconMap[icon as AdminMenuIcon];
	}

	return undefined;
}
