<template>

<v-data-table
    :headers="headers"
    :items="catchReqList"
    :items-per-page="5"
    class="elevation-1"
  ></v-data-table>

</template>

<script>
  const axios = require('axios').default;

  export default {
    name: 'CatchReqList',
    props: {
      value: Object,
      editMode: Boolean,
      isNew: Boolean
    },
    data: () => ({
        headers: [
            { text: "id", value: "id" },
        ],
        catchReqList : [],
    }),
    async created() {
      var temp = await axios.get(axios.backend + '/catchreqlists')

      this.catchReqList = temp.data._embedded.catchreqlists;

    },
    methods: {
    }
  }
</script>

