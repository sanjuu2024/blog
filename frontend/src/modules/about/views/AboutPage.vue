<template>
	<div class="about-page">
		<div class="about-block">
			<h1>关于本站 ——「{{ appTitle }}」</h1>
			<p>
				在刚接触计算机的时候，很多知识都是从网上博客学到的，那些个性化的博客给予我很深的印象，点击就会迸出彩带和个性化语句的光标、背景飘落的花瓣、飘浮着的会随光标组装的几何线条......当时就想着等自己学成了也要做个人博客。
			</p>
			<p>于是乎「青禾边」就这样诞生了。</p>
			<p>
				虽然以前浏览的大多是技术博客，但是我对「青禾边」的定位不止于此，我希望它能成为一个小窝，不仅仅是分享技术（具体内容请参考分类页），也记录我的兴趣爱好和生活。
			</p>
			<p>
				虽然在 2026
				年中就已经筹划着买下了域名等等，但是由于学业、技术不够成熟、<del>偷懒</del>等不可抗拒因素（bushi），该站直到
				x 年 x 月才正式上线。前期为了夯实基础，基本都是手写代码，后期赶上 AI
				大爆发，于是开始尝试使用 codex 等工具编写代码。
			</p>
			<p>本站技术栈：</p>
			<ul>
				<li>前端：Vue3 + Vite + Pinia + Vue Router + Element Plus</li>
				<li>后端：Spring Boot + MyBatis Plus</li>
				<li>数据库：PostgreSQL</li>
				<li>部署：Docker + Nginx</li>
			</ul>
			<p>希望大家在「{{ appTitle }}」浏览快乐~！ψ(｀∇´)ψ</p>
		</div>

		<div class="about-block">
			<h1>关于我 —— sanjuu</h1>
			<p>一名普通的在读大三生。</p>
			<p>
				目前正为了就业还是读研而纠结中，纠结后的结论是不管三七二十一先学东西，能学多少是多少（）
			</p>
			<p>
				网名来自日语的 さんじゅう（罗马音 sanjuu），没有什么特别的原因，只是单纯觉得发音好听
				♪(´▽｀)
			</p>
			<p>
				小时候并没有太多接触到计算机，<del>直到初中看到某本小说中的黑客人设</del>才开始对计算机感兴趣，不过本身也不是什么特别有天赋的人，所以至今也没有什么大作为（）
			</p>
			<p>大学如愿以偿进入了计算机相关专业，也才真正开始了解这个广阔无垠的世界。</p>
			<p>
				平时兴趣爱好比较杂，主要还是看看二次元相关的内容，偶尔看看小说，玩玩游戏。_(:з)∠)_
			</p>
		</div>

		<div class="about-block">
			<h1>联系方式</h1>
			<p>
				如果你有任何问题或者想法，欢迎通过<RouterLink
					to="/messages"
					class="messageboard-link"
					>本站留言板</RouterLink
				>或者以下方式联系我：
			</p>
			<ul>
				<li>邮箱：2269102080@qq.com</li>
			</ul>
		</div>

		<div class="about-block">
			<h1>本站日志</h1>
			<p>记录本站的更新和维护情况。</p>
		</div>
	</div>
</template>

<script setup lang="ts">
defineOptions({
	name: 'AboutPage',
});

const appTitle = import.meta.env.VITE_APP_TITLE;
</script>

<style lang="scss" scoped>
.about-page {
	margin: 0 auto;
	padding-bottom: 3rem;
	max-width: var(--app-about-content-width);
}

.about-block {
	text-align: left;
	line-height: 2rem;
	padding-bottom: 2rem;

	h1 {
		font-size: 2rem;
		font-weight: bold;
		line-height: 5rem;
	}

	ul {
		list-style: disc;
		padding-left: 2rem;
	}

	p {
		margin: 1rem 0;
	}
}

.messageboard-link {
	text-decoration: underline;
	transition: color 0.1s ease-in-out;
	font-weight: bold;

	&:hover,
	&:focus-visible {
		color: var(--app-main);
	}
}
</style>
