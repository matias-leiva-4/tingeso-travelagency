import { Box, Container, Stack, Typography } from '@mui/material'

export function PageHeader({ eyebrow, title, description, action }) {
  return (
    <Box sx={{ bgcolor: 'primary.dark', color: 'primary.contrastText', py: { xs: 5, md: 7 } }}>
      <Container maxWidth="lg">
        <Stack spacing={2} direction={{ xs: 'column', md: 'row' }} justifyContent="space-between">
          <Box sx={{ maxWidth: 760 }}>
            {eyebrow ? (
              <Typography variant="overline" sx={{ color: 'rgba(255,255,255,0.78)' }}>
                {eyebrow}
              </Typography>
            ) : null}
            <Typography variant="h3" component="h1">
              {title}
            </Typography>
            {description ? (
              <Typography sx={{ color: 'rgba(255,255,255,0.82)', mt: 1 }}>{description}</Typography>
            ) : null}
          </Box>
          {action ? <Box>{action}</Box> : null}
        </Stack>
      </Container>
    </Box>
  )
}
