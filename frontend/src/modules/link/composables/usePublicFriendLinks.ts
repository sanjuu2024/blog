import { getPublicFriendLinkSiteProfile, listPublicFriendLinks } from '../api/friendLinkApi';
import type { PublicFriendLinkSection, PublicFriendLinkSiteProfile } from '../types/friendLink';

export function usePublicFriendLinks() {
	const sections = ref<PublicFriendLinkSection[]>([]);
	const siteProfile = ref<PublicFriendLinkSiteProfile | null>(null);
	const loading = ref(true);
	const loadFailed = ref(false);

	async function loadFriendLinks() {
		loading.value = true;
		loadFailed.value = false;
		try {
			const [publicSections, publicSiteProfile] = await Promise.all([
				listPublicFriendLinks(),
				getPublicFriendLinkSiteProfile(),
			]);
			sections.value = publicSections;
			siteProfile.value = publicSiteProfile;
		} catch {
			loadFailed.value = true;
		} finally {
			loading.value = false;
		}
	}

	return { sections, siteProfile, loading, loadFailed, loadFriendLinks };
}
