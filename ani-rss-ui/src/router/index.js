import {createRouter, createWebHashHistory} from 'vue-router'
import DashboardView from '@/view/home/DashboardView.vue'
import SubscriptionView from '@/view/home/SubscriptionView.vue'
import TorrentsInfosView from '@/view/home/TorrentsInfosView.vue'
import LogsView from '@/view/home/LogsView.vue'
import ConfigView from '@/view/home/ConfigView.vue'
import {startupPage} from '@/js/global.js'

const startupPaths = ['/home', '/subscriptions']

const routes = [
    {
        path: '/',
        redirect: () => startupPaths.includes(startupPage.value) ? startupPage.value : '/home'
    },
    {
        path: '/home',
        component: DashboardView
    },
    {
        path: '/subscriptions',
        component: SubscriptionView
    },
    {
        path: '/downloads',
        component: TorrentsInfosView
    },
    {
        path: '/logs',
        component: LogsView
    },
    {
        path: '/settings',
        component: ConfigView
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

export default router
