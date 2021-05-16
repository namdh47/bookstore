
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import CatchManager from "./components/CatchManager"

import PickUpManager from "./components/PickUpManager"

import CatchReqList from "./components/catchReqList"
import PaymentManager from "./components/PaymentManager"

import MyPageManager from "./components/MyPageManager"

import MyPage from "./components/myPage"
export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/Catch',
                name: 'CatchManager',
                component: CatchManager
            },

            {
                path: '/PickUp',
                name: 'PickUpManager',
                component: PickUpManager
            },

            {
                path: '/catchReqList',
                name: 'catchReqList',
                component: catchReqList
            },
            {
                path: '/Payment',
                name: 'PaymentManager',
                component: PaymentManager
            },

            {
                path: '/MyPage',
                name: 'MyPageManager',
                component: MyPageManager
            },

            {
                path: '/myPage',
                name: 'myPage',
                component: myPage
            },


    ]
})
